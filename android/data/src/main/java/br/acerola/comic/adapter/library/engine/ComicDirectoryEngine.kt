package br.acerola.comic.adapter.library.engine

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.adapter.library.DirectoryEngine
import br.acerola.comic.config.preference.ComicDirectoryPreference
import br.acerola.comic.dto.archive.ChapterPageDto
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.error.message.LibrarySyncError
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.local.entity.archive.ArchiveTemplate
import br.acerola.comic.local.entity.archive.ComicDirectory
import br.acerola.comic.local.translator.persistence.toMangaDirectoryEntity
import br.acerola.comic.local.translator.ui.toViewDto
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.pattern.archive.ArchiveFormat
import br.acerola.comic.pattern.media.MediaFile
import br.acerola.comic.service.template.ChapterNameProcessor
import br.acerola.comic.service.template.TemplateMatcher
import br.acerola.comic.util.file.ContentQueryHelper
import br.acerola.comic.util.sort.SortType
import br.acerola.comic.util.template.templateToRegex
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.PatternSyntaxException
import javax.inject.Inject
import javax.inject.Singleton

// FIXME: Fazer responsabilidade unica nesse engine, separar a lógica de scanner em services para que a busca de arquivos sejam feitas atravez de
//  interfaces ao invez de direto no código, usando services externos igual aos outros.
@Singleton
class ComicDirectoryEngine
    @Inject
    constructor(
        private val directoryDao: ComicDirectoryDao,
        private val templateMatcher: TemplateMatcher,
        private val templateService: ChapterNameProcessor,
        @param:ApplicationContext private val context: Context,
    ) : ComicGateway<ComicDirectoryDto> {
        @Inject
        @DirectoryEngine
        lateinit var comicDirectoryOps: ChapterGateway<ChapterPageDto>

        private val _progress = MutableStateFlow(value = -1)
        override val progress: StateFlow<Int> = _progress.asStateFlow()

        private val _isIndexing = MutableStateFlow(value = false)
        override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

        override suspend fun refreshManga(
            comicId: Long,
            baseUri: Uri?,
        ): Either<LibrarySyncError, Unit> =
            withContext(context = Dispatchers.IO) {
                AcerolaLogger.i(TAG, "Syncing specific comic: $comicId", LogSource.REPOSITORY)
                _isIndexing.value = true
                try {
                    Either
                        .catch {
                            val existingManga = directoryDao.getDirectoryById(comicId) ?: return@catch

                            val folderUri = existingManga.path.toUri()
                            val folderDoc = DocumentFile.fromSingleUri(context, folderUri)

                            if (folderDoc == null || !folderDoc.isDirectory) return@catch

                            val rootUri =
                                baseUri ?: ComicDirectoryPreference.folderUriFlow(context).firstOrNull()?.toUri()
                                ?: return@catch

                            val folderId = DocumentsContract.getDocumentId(folderUri)
                            val folderChildren =
                                ContentQueryHelper.listFiles(context, rootUri, folderId).getOrElse { return@catch }

                                    val bannerMetadata = folderChildren.firstOrNull { MediaFile.isBanner(it.name) }
                            val coverMetadata = folderChildren.firstOrNull { MediaFile.isCover(it.name) }

                            val firstChapterName =
                                folderChildren
                                    .firstOrNull {
                                        it.mimeType != DocumentsContract.Document.MIME_TYPE_DIR &&
                                            ArchiveFormat.isSupported(ext = it.name)
                                    }?.name

                            val templates = templateService.getTemplates()
                            val detectedTemplate =
                                firstChapterName?.let {
                                    templateMatcher.detect(it, templates.filter { t -> t.type == SortType.CHAPTER })
                                }

                            val bannerDoc =
                                bannerMetadata?.let {
                                    DocumentFile.fromSingleUri(context, DocumentsContract.buildDocumentUriUsingTree(rootUri, it.id))
                                }

                            val coverDoc =
                                coverMetadata?.let {
                                    DocumentFile.fromSingleUri(context, DocumentsContract.buildDocumentUriUsingTree(rootUri, it.id))
                                }

                            val updatedManga =
                                folderDoc
                                    .toMangaDirectoryEntity(
                                        cover = coverDoc,
                                        banner = bannerDoc,
                                        archiveTemplateFk = detectedTemplate?.id,
                                    ).copy(id = existingManga.id, externalSyncEnabled = existingManga.externalSyncEnabled)

                            directoryDao.update(entity = updatedManga)

                            comicDirectoryOps.refreshComicChapters(comicId = comicId, baseUri = rootUri)
                        }.mapLeft { exception ->
                            AcerolaLogger.e(TAG, "Failed to refresh specific comic: $comicId", LogSource.REPOSITORY, throwable = exception)
                            when (exception) {
                                is PatternSyntaxException -> LibrarySyncError.MalformedLibrary(cause = exception)
                                is SecurityException -> LibrarySyncError.FolderAccessDenied(cause = exception)
                                is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                                else -> LibrarySyncError.UnexpectedError(cause = exception)
                            }
                        }
                } finally {
                    _isIndexing.value = false
                }
            }

        override suspend fun incrementalScan(baseUri: Uri?): Either<LibrarySyncError, Unit> {
            AcerolaLogger.i(TAG, "Starting incremental library scan", LogSource.REPOSITORY)
            _isIndexing.value = true
            try {
                return withContext(context = Dispatchers.IO) {
                    Either
                        .catch {
                            if (baseUri === null) return@catch

                            val discoveredFolders: List<ComicDirectory> = buildLibrary(context, rootUri = baseUri)
                            val databaseFolders: List<ComicDirectory> =
                                directoryDao.getAllDirectories().firstOrNull() ?: emptyList()

                            if (discoveredFolders.isEmpty() && databaseFolders.isEmpty()) {
                                _progress.value = -1
                                return@catch
                            }

                            val existingFoldersMap = databaseFolders.associateBy { normalizeName(it.name) }
                            val foldersMap = discoveredFolders.associateBy { normalizeName(it.name) }

                            val foldersToProcess =
                                discoveredFolders.filter { folder ->
                                    val normalizedName = normalizeName(folder.name)
                                    val existing = existingFoldersMap[normalizedName]

                                    when {
                                        existing == null -> true
                                        existing.path != folder.path -> true
                                        existing.lastModified < folder.lastModified -> true
                                        existing.cover != folder.cover || existing.banner != folder.banner -> true
                                        else -> false
                                    }
                                }

                            val removedFolders = databaseFolders.filter { normalizeName(it.name) !in foldersMap }

                            if (removedFolders.isNotEmpty()) {
                                AcerolaLogger.d(TAG, "Removing ${removedFolders.size} stale folders from DB", LogSource.REPOSITORY)
                                removedFolders.forEach { folder ->
                                    directoryDao.delete(entity = folder)
                                }
                            }

                            AcerolaLogger.d(TAG, "Processing ${foldersToProcess.size} new/updated folders", LogSource.REPOSITORY)
                            processFolderList(foldersToProcess, baseUri = baseUri)
                        }.mapLeft { exception ->
                            AcerolaLogger.e(TAG, "Incremental scan failed", LogSource.REPOSITORY, throwable = exception)
                            when (exception) {
                                is IOException -> LibrarySyncError.DiskIOFailure(path = baseUri.toString(), exception)
                                is PatternSyntaxException -> LibrarySyncError.MalformedLibrary(cause = exception)
                                is SecurityException -> LibrarySyncError.FolderAccessDenied(cause = exception)
                                is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                                else -> LibrarySyncError.UnexpectedError(cause = exception)
                            }
                        }
                }
            } finally {
                _isIndexing.value = false
            }
        }

        override suspend fun refreshLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit> {
            AcerolaLogger.i(TAG, "Starting refresh library scan", LogSource.REPOSITORY)
            _isIndexing.value = true
            try {
                return withContext(context = Dispatchers.IO) {
                    Either
                        .catch {
                            if (baseUri === null) return@catch

                            val foldersToProcess: List<ComicDirectory> = buildLibrary(context, rootUri = baseUri)
                            if (foldersToProcess.isEmpty()) {
                                _progress.value = -1
                                return@catch
                            }

                            AcerolaLogger.d(TAG, "Refreshing ${foldersToProcess.size} folders", LogSource.REPOSITORY)
                            processFolderList(foldersToProcess, baseUri = baseUri)
                        }.mapLeft { exception ->
                            AcerolaLogger.e(TAG, "Refresh library failed", LogSource.REPOSITORY, throwable = exception)
                            when (exception) {
                                is IOException -> LibrarySyncError.DiskIOFailure(path = baseUri.toString(), exception)
                                is PatternSyntaxException -> LibrarySyncError.MalformedLibrary(cause = exception)
                                is SecurityException -> LibrarySyncError.FolderAccessDenied(cause = exception)
                                is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                                else -> LibrarySyncError.UnexpectedError(cause = exception)
                            }
                        }
                }
            } finally {
                _isIndexing.value = false
            }
        }

        override suspend fun rebuildLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit> {
            AcerolaLogger.i(TAG, "Starting deep rebuild of library", LogSource.REPOSITORY)
            _isIndexing.value = true
            try {
                return withContext(context = Dispatchers.IO) {
                    refreshLibrary(baseUri).flatMap {
                        Either
                            .catch {
                                val allFolders = directoryDao.getVisibleDirectories().firstOrNull() ?: emptyList()

                                if (allFolders.isEmpty()) {
                                    _progress.value = -1
                                    return@catch
                                }

                                val total = allFolders.size
                                AcerolaLogger.d(TAG, "Deep scanning chapters for $total comics", LogSource.REPOSITORY)

                                val processed = AtomicInteger(0)
                                _progress.value = 0

                                allFolders.chunked(CHUNK_SIZE).forEach { batch ->
                                    coroutineScope {
                                        batch
                                            .map { folder ->
                                                async(context = Dispatchers.IO) {
                                                    try {
                                                        comicDirectoryOps
                                                            .refreshComicChapters(
                                                                comicId = folder.id,
                                                                baseUri = baseUri,
                                                            ).onLeft {
                                                                AcerolaLogger.e(
                                                                    TAG,
                                                                    "Error scanning chapters for ${folder.name}",
                                                                    LogSource.REPOSITORY,
                                                                    throwable = null,
                                                                )
                                                            }
                                                    } finally {
                                                        val current = processed.incrementAndGet()
                                                        _progress.value = ((current.toFloat() / total) * 100).toInt()
                                                    }
                                                }
                                            }.awaitAll()
                                    }
                                }
                                _progress.value = 100
                                delay(timeMillis = 250)

                                _progress.value = -1
                            }.mapLeft { exception ->
                                AcerolaLogger.e(TAG, "Deep rebuild failed", LogSource.REPOSITORY, throwable = exception)
                                when (exception) {
                                    is IOException -> LibrarySyncError.DiskIOFailure(path = baseUri.toString(), exception)
                                    is PatternSyntaxException -> LibrarySyncError.MalformedLibrary(cause = exception)
                                    is SecurityException -> LibrarySyncError.FolderAccessDenied(cause = exception)
                                    is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                                    else -> LibrarySyncError.UnexpectedError(cause = exception)
                                }
                            }
                    }
                }
            } finally {
                _isIndexing.value = false
            }
        }

        override fun observeLibrary(): Flow<List<ComicDirectoryDto>> =
            directoryDao.getAllDirectories().map { folders ->
                AcerolaLogger.d(TAG, "Observed directory list update: ${folders.size} folders", LogSource.REPOSITORY)
                folders.map { it.toViewDto() }
            }

        private suspend fun processFolderList(
            foldersToProcess: List<ComicDirectory>,
            baseUri: Uri? = null,
        ) {
            if (foldersToProcess.isEmpty()) {
                _progress.value = -1
                return
            }

            val total = foldersToProcess.size
            val showProgress = total >= PROGRESS_THRESHOLD

            if (!showProgress) {
                foldersToProcess.chunked(CHUNK_SIZE).forEach { batch ->
                    coroutineScope {
                        batch
                            .map { folder ->
                                async(context = Dispatchers.IO) {
                                    upsertFolder(folder = folder, baseUri = baseUri)
                                }
                            }.awaitAll()
                    }
                }

                delay(timeMillis = 250)
                _progress.value = -1
                return
            }

            val processed = AtomicInteger(0)
            _progress.value = 0
            foldersToProcess.chunked(CHUNK_SIZE).forEach { batch ->
                coroutineScope {
                    batch
                        .map { folder ->
                            async(context = Dispatchers.IO) {
                                try {
                                    upsertFolder(folder = folder, baseUri = baseUri)
                                } finally {
                                    val current = processed.incrementAndGet()
                                    _progress.value = ((current.toFloat() / total) * 100).toInt()
                                }
                            }
                        }.awaitAll()
                }
            }

            _progress.value = 100
            delay(timeMillis = 250)
            _progress.value = -1
        }

        private suspend fun upsertFolder(
            baseUri: Uri? = null,
            folder: ComicDirectory,
        ) {
            val finalMangaId =
                directoryDao.upsertDirectoryTransaction(folder) {
                    it.filter { char -> char.isLetterOrDigit() }.lowercase()
                }

            comicDirectoryOps.refreshComicChapters(comicId = finalMangaId, baseUri = baseUri)
        }

        private suspend fun buildLibrary(
            context: Context,
            rootUri: Uri,
        ): List<ComicDirectory> {
            val comicDirectories = mutableListOf<ComicDirectory>()
            val templates = templateService.getTemplates()

            val rootDocId = DocumentsContract.getTreeDocumentId(rootUri)
            scanRecursive(context, rootUri, rootDocId, templates, comicDirectories)

            return comicDirectories
        }

        private fun scanRecursive(
            context: Context,
            rootUri: Uri,
            currentDocId: String,
            templates: List<ArchiveTemplate>,
            comicDirectories: MutableList<ComicDirectory>,
        ) {
            val children = ContentQueryHelper.listFiles(context, rootUri, currentDocId).getOrElse { return }

            val hasMangaFiles =
                children.any {
                    it.mimeType != DocumentsContract.Document.MIME_TYPE_DIR &&
                        ArchiveFormat.isSupported(it.name)
                }

            val subDirs = children.filter { it.mimeType == DocumentsContract.Document.MIME_TYPE_DIR }
            val hasVolumeSubDirs =
                subDirs.any { subDir ->
                    val volumeTemplates = templates.filter { it.type == SortType.VOLUME }.map { it.pattern }
                    val isVol = isVolumeName(subDir.name, volumeTemplates)
                    isVol && folderContainsManga(context, rootUri, subDir.id)
                }

            if (!hasMangaFiles && !hasVolumeSubDirs) {
                subDirs.forEach { subDir ->
                    scanRecursive(context, rootUri, subDir.id, templates, comicDirectories)
                }
                return
            }

            val currentUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, currentDocId)
            val folderDoc = DocumentFile.fromSingleUri(context, currentUri) ?: return

            val coverMetadata = children.firstOrNull { MediaFile.isCover(it.name) }
            val bannerMetadata = children.firstOrNull { MediaFile.isBanner(it.name) }
            val firstChapterName = children.firstOrNull { ArchiveFormat.isSupported(it.name) }?.name

            val detectedTemplate =
                firstChapterName?.let {
                    templateMatcher.detect(it, templates.filter { t -> t.type == SortType.CHAPTER })
                }

            val comicDir =
                folderDoc.toMangaDirectoryEntity(
                    cover =
                        coverMetadata?.let {
                            DocumentFile.fromSingleUri(
                                context,
                                DocumentsContract.buildDocumentUriUsingTree(rootUri, it.id),
                            )
                        },
                    banner =
                        bannerMetadata?.let {
                            DocumentFile.fromSingleUri(
                                context,
                                DocumentsContract.buildDocumentUriUsingTree(rootUri, it.id),
                            )
                        },
                    archiveTemplateFk = detectedTemplate?.id,
                )

            comicDirectories.add(comicDir)
        }

        private fun isVolumeName(
            name: String,
            volumeTemplates: List<String>,
        ): Boolean =
            volumeTemplates.any { template ->
                templateToRegex(template)
                    .containsMatchIn(name)
            } ||
                name.startsWith("Vol", ignoreCase = true) ||
                name.startsWith("V0", ignoreCase = true)

        private fun folderContainsManga(
            context: Context,
            rootUri: Uri,
            docId: String,
        ): Boolean {
            val children = ContentQueryHelper.listFiles(context, rootUri, docId).getOrElse { return false }
            if (children.any { it.isFile && ArchiveFormat.isSupported(it.name) }) return true

            return children.filter { it.mimeType == DocumentsContract.Document.MIME_TYPE_DIR }.any { folderContainsManga(context, rootUri, it.id) }
        }

        private fun normalizeName(name: String): String = name.filter { it.isLetterOrDigit() }.lowercase()

        companion object {
            private const val TAG = "ComicDirectoryEngine"
            const val PROGRESS_THRESHOLD = 5
            const val CHUNK_SIZE = 50
        }
    }
