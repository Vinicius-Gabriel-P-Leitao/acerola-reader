package br.acerola.manga.adapter.library

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import br.acerola.manga.adapter.contract.ChapterPort
import br.acerola.manga.adapter.contract.MangaPort
import br.acerola.manga.config.preference.MangaDirectoryPreference
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.entity.archive.MangaDirectory
import br.acerola.manga.local.translator.toDto
import br.acerola.manga.local.translator.toMangaDirectoryModel
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.pattern.ArchiveFormatPattern
import br.acerola.manga.util.ContentQueryHelper
import br.acerola.manga.util.detectTemplate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.PatternSyntaxException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.get

@Singleton
class MangaDirectoryEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val directoryDao: MangaDirectoryDao,
) : MangaPort<MangaDirectoryDto> {

    private val semaphore = Semaphore(permits = 3)

    @Inject
    @DirectoryEngine
    lateinit var mangaDirectoryOps: ChapterPort<ChapterArchivePageDto>

    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    override suspend fun refreshManga(mangaId: Long, baseUri: Uri?): Either<LibrarySyncError, Unit> =
        withContext(context = Dispatchers.IO) {
            AcerolaLogger.i(TAG, "Syncing specific manga: $mangaId", LogSource.REPOSITORY)
            _isIndexing.value = true
            try {
                Either.catch {
                    val existingManga = directoryDao.getMangaDirectoryById(mangaId) ?: return@catch

                    val folderUri = existingManga.path.toUri()
                    val folderDoc = DocumentFile.fromSingleUri(context, folderUri)

                    if (folderDoc == null || !folderDoc.isDirectory) return@catch

                    if (existingManga.lastModified >= folderDoc.lastModified()) {
                        mangaDirectoryOps.refreshMangaChapters(mangaId = mangaId, baseUri = baseUri)
                        return@catch
                    }

                    val rootUri = baseUri ?: MangaDirectoryPreference.folderUriFlow(context).firstOrNull()?.toUri()
                        ?: return@catch

                    val folderId = DocumentsContract.getDocumentId(folderUri)
                    val folderChildren =
                        ContentQueryHelper.listFiles(context, rootUri, folderId).getOrElse { return@catch }

                    val bannerMetadata = folderChildren.firstOrNull { isBanner(it.name) }
                    val coverMetadata = folderChildren.firstOrNull { isCover(it.name) }
                    val hasComicInfo = folderChildren.any { it.name == "ComicInfo.xml" }

                    val firstChapterName = folderChildren.firstOrNull {
                        it.mimeType != DocumentsContract.Document.MIME_TYPE_DIR && ArchiveFormatPattern.isSupported(ext = it.name)
                    }?.name

                    val detectedTemplate = firstChapterName?.let {
                        detectTemplate(fileName = it)
                    }

                    val bannerDoc = bannerMetadata?.let {
                        DocumentFile.fromSingleUri(context, DocumentsContract.buildDocumentUriUsingTree(rootUri, it.id))
                    }
                    val coverDoc = coverMetadata?.let {
                        DocumentFile.fromSingleUri(context, DocumentsContract.buildDocumentUriUsingTree(rootUri, it.id))
                    }

                    val updatedManga = folderDoc.toMangaDirectoryModel(
                        coverDoc, bannerDoc, chapterTemplate = detectedTemplate, hasComicInfo = hasComicInfo
                    ).copy(id = existingManga.id)

                    directoryDao.update(entity = updatedManga)

                    mangaDirectoryOps.refreshMangaChapters(mangaId = mangaId, baseUri = rootUri)
                }.mapLeft { exception ->
                    AcerolaLogger.e(TAG, "Failed to refresh specific manga: $mangaId", LogSource.REPOSITORY, throwable = exception)
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
                Either.catch {
                    if (baseUri === null) return@catch

                    val discoveredFolders: List<MangaDirectory> = buildLibrary(context, rootUri = baseUri)
                    val databaseFolders: List<MangaDirectory> =
                        directoryDao.getAllMangaDirectory().firstOrNull() ?: emptyList()

                    if (discoveredFolders.isEmpty() && databaseFolders.isEmpty()) {
                        _progress.value = -1
                        return@catch
                    }

                    val existingFoldersMap = databaseFolders.associateBy { normalizeName(it.name) }
                    val foldersMap = discoveredFolders.associateBy { normalizeName(it.name) }

                    val foldersToProcess = discoveredFolders.filter { folder ->
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
                    processFolderList(foldersToProcess, existingFolders = databaseFolders, baseUri = baseUri)
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
                Either.catch {
                    if (baseUri === null) return@catch

                    val foldersToProcess: List<MangaDirectory> = buildLibrary(context, rootUri = baseUri)
                    if (foldersToProcess.isEmpty()) {
                        _progress.value = -1
                        return@catch
                    }

                    val existingFolders = directoryDao.getAllMangaDirectory().firstOrNull() ?: emptyList()
                    AcerolaLogger.d(TAG, "Refreshing ${foldersToProcess.size} folders", LogSource.REPOSITORY)
                    processFolderList(foldersToProcess, existingFolders, baseUri = baseUri)
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
                    Either.catch {
                        val allFolders = directoryDao.getAllMangaDirectory().firstOrNull() ?: emptyList()

                        if (allFolders.isEmpty()) {
                            _progress.value = -1
                            return@catch
                        }

                        val total = allFolders.size
                        AcerolaLogger.d(TAG, "Deep scanning chapters for $total mangas", LogSource.REPOSITORY)

                        val processed = AtomicInteger(0)
                        _progress.value = 0

                        allFolders.chunked(CHUNK_SIZE).forEach { batch ->
                            coroutineScope {
                                batch.map { folder ->
                                    async(context = Dispatchers.IO) {
                                        try {
                                            mangaDirectoryOps.refreshMangaChapters(mangaId = folder.id, baseUri = baseUri).onLeft {
                                                AcerolaLogger.e(TAG, "Error scanning chapters for ${folder.name}", LogSource.REPOSITORY, throwable = null)
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

    override fun observeLibrary(): StateFlow<List<MangaDirectoryDto>> {
        return directoryDao.getAllMangaDirectory().map { folders ->
            AcerolaLogger.d(TAG, "Observed directory list update: ${folders.size} folders", LogSource.REPOSITORY)
            coroutineScope {
                folders.map { folder ->
                    async(context = Dispatchers.IO) {
                        folder.toDto()
                    }
                }.awaitAll()
            }
        }.stateIn(
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
    }

    private suspend fun processFolderList(
        foldersToProcess: List<MangaDirectory>,
        existingFolders: List<MangaDirectory>,
        baseUri: Uri? = null
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
                    batch.map { folder ->
                        async(context = Dispatchers.IO) {
                            upsertFolder(folder, existingFolders, baseUri)
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
                batch.map { folder ->
                    async(context = Dispatchers.IO) {
                        try {
                            upsertFolder(folder, existingFolders, baseUri)
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
        folder: MangaDirectory,
        existingFolders: List<MangaDirectory>,
        baseUri: Uri? = null
    ) {
        val normalizedName = normalizeName(folder.name)
        val existing = existingFolders.find { normalizeName(it.name) == normalizedName }

        val finalMangaId = if (existing != null) {
            val updated = folder.copy(id = existing.id)
            directoryDao.update(entity = updated)
            existing.id
        } else {
            directoryDao.insert(entity = folder)
        }

        mangaDirectoryOps.refreshMangaChapters(mangaId = finalMangaId, baseUri = baseUri)
    }

    private fun buildLibrary(context: Context, rootUri: Uri): List<MangaDirectory> {
        val pickedDir = DocumentFile.fromTreeUri(context, rootUri) ?: return emptyList()

        val allChildren = ContentQueryHelper.listFiles(context, rootUri).getOrElse { return emptyList() }
        val folders = allChildren.filter { it.mimeType == DocumentsContract.Document.MIME_TYPE_DIR }

        return folders.map { folderMetadata ->
            val folderUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, folderMetadata.id)
            val folderDoc = DocumentFile.fromSingleUri(context, folderUri) ?: return@map null

            val folderChildren =
                ContentQueryHelper.listFiles(context, rootUri, folderMetadata.id).getOrElse { return@map null }

            val bannerMetadata = folderChildren.firstOrNull { isBanner(it.name) }
            val coverMetadata = folderChildren.firstOrNull { isCover(it.name) }
            val hasComicInfo = folderChildren.any { it.name == "ComicInfo.xml" }

            val firstChapterName = folderChildren.firstOrNull {
                ArchiveFormatPattern.isSupported(ext = it.name)
            }?.name

            val detectedTemplate = firstChapterName?.let {
                detectTemplate(fileName = it)
            }

            MangaDirectory(
                name = folderMetadata.name,
                path = folderUri.toString(),
                cover = coverMetadata?.let { DocumentsContract.buildDocumentUriUsingTree(rootUri, it.id).toString() },
                banner = bannerMetadata?.let { DocumentsContract.buildDocumentUriUsingTree(rootUri, it.id).toString() },
                chapterTemplate = detectedTemplate,
                lastModified = folderMetadata.lastModified,
                hasComicInfo = hasComicInfo,
            )
        }.filterNotNull()
    }

    private fun isCover(name: String?): Boolean {
        if (name == null) return false
        val lower = name.lowercase()
        if (lower.contains("cover") && isImage(lower)) return true
        return (lower.startsWith("folder") || lower.startsWith("front") || lower.startsWith("00")) && isImage(lower)
    }

    private fun isBanner(name: String?): Boolean {
        if (name == null) return false
        val lower = name.lowercase()
        return lower.contains("banner") && isImage(lower)
    }

    private fun isImage(name: String): Boolean {
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")
    }

    private fun normalizeName(name: String): String {
        return name.filter { it.isLetterOrDigit() }.lowercase()
    }

    companion object {
        private const val TAG = "MangaDirectoryRepository"
        const val CHUNK_SIZE = 50
        const val PROGRESS_THRESHOLD = 5
    }
}
