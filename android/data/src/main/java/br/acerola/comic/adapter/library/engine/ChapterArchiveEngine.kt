package br.acerola.comic.adapter.library.engine

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import arrow.core.getOrElse
import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import br.acerola.comic.dto.archive.ChapterPageDto
import br.acerola.comic.error.message.LibrarySyncError
import br.acerola.comic.local.dao.archive.ChapterArchiveDao
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.local.entity.archive.ArchiveTemplate
import br.acerola.comic.local.translator.ui.toChapterPageDto
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.pattern.archive.ArchiveFormat
import br.acerola.comic.service.archive.ArchiveValidator
import br.acerola.comic.service.archive.ChapterSyncService
import br.acerola.comic.service.archive.VolumeSyncService
import br.acerola.comic.service.compact.PdfToCbzConverter
import br.acerola.comic.service.template.ChapterNameProcessor
import br.acerola.comic.util.file.ContentQueryHelper
import br.acerola.comic.util.file.FastFileMetadata
import br.acerola.comic.util.file.toFastMetadata
import br.acerola.comic.util.sort.SortType
import br.acerola.comic.util.template.templateToRegex
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterArchiveEngine
    @Inject
    constructor(
        private val directoryDao: ComicDirectoryDao,
        private val chapterArchiveDao: ChapterArchiveDao,
        private val templateService: ChapterNameProcessor,
        @param:ApplicationContext private val context: Context,
        private val pdfToCbzConverterService: PdfToCbzConverter,
        private val archiveValidator: ArchiveValidator,
        private val volumeSyncService: VolumeSyncService,
        private val chapterSyncService: ChapterSyncService,
    ) : ChapterGateway<ChapterPageDto> {
        private val _progress = MutableStateFlow(value = -1)
        override val progress: StateFlow<Int> = _progress.asStateFlow()

        private val _isIndexing = MutableStateFlow(value = false)
        override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

        override suspend fun refreshComicChapters(
            comicId: Long,
            baseUri: Uri?,
        ): Either<LibrarySyncError, Unit> =
            withContext(context = Dispatchers.IO) {
                AcerolaLogger.i(TAG, "Starting hierarchical sync for comicId: $comicId", LogSource.REPOSITORY)
                _isIndexing.value = true
                _progress.value = 0

                val result =
                    Either
                        .catch {
                            val folder = directoryDao.getDirectoryById(comicId = comicId) ?: return@catch
                            val folderUri = folder.path.toUri()

                            val rootChildren: List<FastFileMetadata>
                            val folderDoc: DocumentFile

                            if (baseUri != null) {
                                val folderDocId = DocumentsContract.getDocumentId(folderUri)
                                rootChildren = ContentQueryHelper.listFiles(context, baseUri, folderDocId).getOrElse { return@catch }
                                folderDoc = DocumentFile.fromTreeUri(context, folderUri) ?: return@catch
                            } else {
                                folderDoc = DocumentFile.fromSingleUri(context, folderUri) ?: return@catch
                                rootChildren = folderDoc.listFiles().map { it.toFastMetadata() }
                            }

                            val allTemplates = templateService.getTemplates()
                            val volumeTemplates = allTemplates.filter { it.type == SortType.VOLUME }.map { it.pattern }
                            val chapterTemplates = allTemplates.filter { it.type == SortType.CHAPTER }.map { it.pattern }

                            // 1. Sync volumes
                            val subFolders = rootChildren.filter { it.mimeType == DocumentsContract.Document.MIME_TYPE_DIR }
                            val volumeMap =
                                volumeSyncService.sync(
                                    comicId = comicId,
                                    subFolders = subFolders,
                                    volumeTemplates = volumeTemplates,
                                    baseUri = baseUri,
                                    folderUri = folderUri,
                                )

                            // 2. Detect best template
                            var activeTemplate = folder.archiveTemplateFk?.let { id -> allTemplates.find { it.id == id } }
                            val initialFilenames = collectAllFilenames(rootChildren, subFolders, baseUri, folderUri)
                            if (activeTemplate == null && initialFilenames.isNotEmpty()) {
                                AcerolaLogger.d(TAG, "Detecting best template for comic: ${folder.name}", LogSource.REPOSITORY)
                                activeTemplate =
                                    findBestTemplate(initialFilenames, allTemplates)
                                        ?: allTemplates.find { it.id == -2L } ?: allTemplates.firstOrNull()
                                if (activeTemplate != null) directoryDao.update(folder.copy(archiveTemplateFk = activeTemplate.id))
                            }

                            val defaultPattern = chapterTemplates.firstOrNull() ?: "{chapter}{decimal}.*.{extension}"
                            val chapterRegex = templateToRegex(template = activeTemplate?.pattern ?: defaultPattern)

                            // 3. PDF to CBZ conversion
                            val foldersToProcess = mutableListOf<Pair<DocumentFile, List<FastFileMetadata>>>()
                            foldersToProcess.add(folderDoc to rootChildren)
                            subFolders.forEach { subFolder ->
                                val subDoc = folderDoc.findFile(subFolder.name) ?: return@forEach
                                foldersToProcess.add(subDoc to listFilesMetadata(baseUri, folderUri, subFolder.id))
                            }

                            var needsGlobalRefresh = false
                            foldersToProcess.forEach { (dir, children) ->
                                val pdfFiles = children.filter { it.name.endsWith(ArchiveFormat.PDF.extension, ignoreCase = true) }
                                if (pdfFiles.isEmpty()) return@forEach

                                AcerolaLogger.d(TAG, "Checking ${pdfFiles.size} PDF files in: ${dir.name}", LogSource.REPOSITORY)
                                val cbzNames = children.map { it.name }.toSet()

                                pdfFiles.forEach { pdf ->
                                    val targetCbzName = pdf.name.substringBeforeLast('.') + ArchiveFormat.CBZ.extension
                                    if (!archiveValidator.isPdfConversionEligible(targetCbzName, cbzNames, chapterRegex)) {
                                        AcerolaLogger.v(TAG, "Skipping PDF conversion: ${pdf.name}", LogSource.REPOSITORY)
                                        return@forEach
                                    }

                                    val pdfDocUri =
                                        if (baseUri != null) {
                                            DocumentsContract.buildDocumentUriUsingTree(baseUri, pdf.id)
                                        } else {
                                            pdf.id.toUri()
                                        }
                                    val pdfDoc = DocumentFile.fromSingleUri(context, pdfDocUri) ?: return@forEach

                                    AcerolaLogger.i(TAG, "Converting: ${pdf.name} -> $targetCbzName in ${dir.name}", LogSource.REPOSITORY)
                                    pdfToCbzConverterService
                                        .convertPdfToCbz(dir, pdfDoc, targetCbzName)
                                        .onRight {
                                            AcerolaLogger.i(TAG, "Converted: $targetCbzName", LogSource.REPOSITORY)
                                            needsGlobalRefresh = true
                                        }.onLeft {
                                            AcerolaLogger.e(TAG, "PDF conversion failed: ${pdf.name}", LogSource.REPOSITORY)
                                        }
                                }
                            }

                            // 4. Collect chapter files
                            val finalRootChildren =
                                if (needsGlobalRefresh && baseUri != null) {
                                    val folderDocId = DocumentsContract.getDocumentId(folderUri)
                                    ContentQueryHelper.listFiles(context, baseUri, folderDocId).getOrElse { rootChildren }
                                } else {
                                    rootChildren
                                }

                            val allChapterFiles = mutableListOf<Pair<FastFileMetadata, Long?>>()
                            finalRootChildren
                                .filter { it.isFile && ArchiveFormat.isIndexable(it.name) }
                                .forEach { allChapterFiles.add(it to null) }

                            subFolders.forEach { subFolder ->
                                val volPath =
                                    if (baseUri != null) {
                                        DocumentsContract.buildDocumentUriUsingTree(baseUri, subFolder.id).toString()
                                    } else {
                                        DocumentsContract.buildDocumentUriUsingTree(folderUri, subFolder.id).toString()
                                    }
                                val volId = volumeMap[volPath] ?: return@forEach
                                val subFolderChildren =
                                    if (needsGlobalRefresh && baseUri != null) {
                                        ContentQueryHelper.listFiles(context, baseUri, subFolder.id).getOrElse { emptyList() }
                                    } else {
                                        listFilesMetadata(baseUri, folderUri, subFolder.id)
                                    }
                                subFolderChildren
                                    .filter { it.isFile && ArchiveFormat.isIndexable(it.name) }
                                    .forEach { allChapterFiles.add(it to volId) }
                            }

                            // 5. Sync chapters
                            chapterSyncService.sync(
                                comicId = comicId,
                                allChapterFiles = allChapterFiles,
                                chapterTemplates = chapterTemplates,
                                baseUri = baseUri,
                                folderUri = folderUri,
                                onProgress = { _progress.value = it },
                            )

                            val folderLastModified = if (baseUri == null) folderDoc.lastModified() else 0
                            if (folderLastModified > 0 && folder.lastModified < folderLastModified) {
                                directoryDao.update(entity = folder.copy(lastModified = folderLastModified))
                            }

                            _progress.value = 100
                        }.mapLeft { exception ->
                            AcerolaLogger.e(TAG, "Sync failed", LogSource.REPOSITORY, throwable = exception)
                            when (exception) {
                                is IOException -> LibrarySyncError.DiskIOFailure(path = "Unknown", cause = exception)
                                is SecurityException -> LibrarySyncError.FolderAccessDenied(cause = exception)
                                is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                                else -> LibrarySyncError.UnexpectedError(cause = exception)
                            }
                        }

                _isIndexing.value = false
                _progress.value = -1
                result
            }

        private fun collectAllFilenames(
            rootChildren: List<FastFileMetadata>,
            subFolders: List<FastFileMetadata>,
            baseUri: Uri?,
            folderUri: Uri,
        ): List<String> {
            val names = mutableListOf<String>()
            names.addAll(rootChildren.map { it.name })
            subFolders.forEach { names.addAll(listFilesMetadata(baseUri, folderUri, it.id).map { f -> f.name }) }
            return names
        }

        private fun findBestTemplate(
            filenames: List<String>,
            templates: List<ArchiveTemplate>,
        ): ArchiveTemplate? {
            if (filenames.isEmpty()) return null
            val counts =
                templates.associateWith { template ->
                    val regex = templateToRegex(template.pattern)
                    filenames.count { filename ->
                        regex.matches(filename) ||
                            regex.matches(filename.substringBeforeLast(".") + ArchiveFormat.CBZ.extension)
                    }
                }
            return counts.entries
                .filter { it.value > 0 }
                .sortedWith(
                    compareByDescending<Map.Entry<ArchiveTemplate, Int>> { it.value }
                        .thenByDescending { it.key.id > 0 }
                        .thenByDescending { it.key.id },
                ).firstOrNull()
                ?.key
        }

        override fun observeChapters(
            comicId: Long,
            sortType: String,
            isAscending: Boolean,
        ): StateFlow<ChapterPageDto> =
            chapterArchiveDao
                .getChaptersByDirectoryId(folderId = comicId)
                .map { list ->
                    AcerolaLogger.d(TAG, "Observed chapter list update: ${list.size} chapters", LogSource.REPOSITORY)
                    val finalList =
                        if (sortType == "LAST_UPDATE") {
                            val base = list.sortedBy { it.chapter.lastModified }
                            if (isAscending) base else base.reversed()
                        } else {
                            if (isAscending) list else list.reversed()
                        }
                    finalList.toChapterPageDto()
                }.stateIn(
                    started = SharingStarted.Lazily,
                    scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
                    initialValue = ChapterPageDto(items = emptyList(), pageSize = -1, total = 0, page = 0),
                )

        override suspend fun getChapterPage(
            comicId: Long,
            total: Int,
            page: Int,
            pageSize: Int,
            sortType: String,
            isAscending: Boolean,
        ): ChapterPageDto {
            AcerolaLogger.d(TAG, "Retrieving chapter page: $page (pageSize: $pageSize, sort: $sortType, asc: $isAscending)", LogSource.REPOSITORY)

            return if (sortType == "NUMBER") {
                val offset = page * pageSize
                val realTotal = if (total > 0) total else chapterArchiveDao.countByDirectoryId(folderId = comicId)
                val items =
                    if (isAscending) {
                        chapterArchiveDao.getChaptersByDirectoryPaged(folderId = comicId, pageSize = pageSize, offset = offset)
                    } else {
                        chapterArchiveDao.getChaptersByDirectoryPagedDesc(folderId = comicId, pageSize = pageSize, offset = offset)
                    }
                items.toChapterPageDto(pageSize = pageSize, total = realTotal, page = page)
            } else {
                val flowList = chapterArchiveDao.getChaptersByDirectoryId(comicId).first()
                val sortedList =
                    if (sortType == "LAST_UPDATE") {
                        val base = flowList.sortedBy { it.chapter.lastModified }
                        if (isAscending) base else base.reversed()
                    } else {
                        if (isAscending) flowList else flowList.reversed()
                    }
                val realTotal = sortedList.size
                val start = (page * pageSize).coerceIn(0, realTotal)
                val end = (start + pageSize).coerceIn(0, realTotal)
                val pagedList = if (start < realTotal) sortedList.subList(start, end) else emptyList()
                pagedList.toChapterPageDto(pageSize = pageSize, total = realTotal, page = page)
            }
        }

        fun observeAllChapterCounts(): Flow<Map<Long, Int>> =
            chapterArchiveDao.getChapterCountsByDirectory().map { list ->
                list.associate { it.comicDirectoryFk to it.count }
            }

        private fun listFilesMetadata(
            baseUri: Uri?,
            folderUri: Uri,
            docId: String,
        ): List<FastFileMetadata> {
            if (baseUri != null) {
                return ContentQueryHelper.listFiles(context, baseUri, docId).getOrElse { emptyList() }
            }
            val uri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId)
            return DocumentFile.fromSingleUri(context, uri)?.listFiles()?.map { it.toFastMetadata() } ?: emptyList()
        }

        companion object {
            private const val TAG = "ChapterArchiveEngine"
        }
    }
