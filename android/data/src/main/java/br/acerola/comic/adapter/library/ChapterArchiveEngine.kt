package br.acerola.comic.adapter.library

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import arrow.core.getOrElse
import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import br.acerola.comic.adapter.contract.gateway.VolumeChapterGateway
import br.acerola.comic.dto.archive.ChapterArchivePageDto
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.VolumeChapterGroupDto
import br.acerola.comic.error.message.LibrarySyncError
import br.acerola.comic.local.dao.archive.ChapterArchiveDao
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.local.dao.archive.VolumeArchiveDao
import br.acerola.comic.local.entity.archive.ChapterArchive
import br.acerola.comic.local.entity.archive.ChapterTemplate
import br.acerola.comic.local.translator.persistence.toChapterArchiveEntity
import br.acerola.comic.local.translator.persistence.toVolumeArchiveEntity
import br.acerola.comic.local.translator.ui.toGroupDto
import br.acerola.comic.local.translator.ui.toViewDto
import br.acerola.comic.local.translator.ui.toViewPageDto
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.pattern.ArchiveFormatPattern
import br.acerola.comic.pattern.ChapterTemplatePattern
import br.acerola.comic.pattern.MediaFilePattern
import br.acerola.comic.service.compact.PdfToCbzConverter
import br.acerola.comic.service.template.ChapterNameProcessor
import br.acerola.comic.util.ContentQueryHelper
import br.acerola.comic.util.FastFileMetadata
import br.acerola.comic.util.SortNormalizer
import br.acerola.comic.util.SortType
import br.acerola.comic.util.templateToRegex
import br.acerola.comic.util.toFastMetadata
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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// FIXME: VolumeChapterGateway Implementação errada isso deve virar uma classe nova.
@Singleton
class ChapterArchiveEngine
    @Inject
    constructor(
        private val directoryDao: ComicDirectoryDao,
        private val chapterArchiveDao: ChapterArchiveDao,
        private val volumeArchiveDao: VolumeArchiveDao,
        private val templateService: ChapterNameProcessor,
        @param:ApplicationContext private val context: Context,
        private val pdfToCbzConverterService: PdfToCbzConverter,
    ) : ChapterGateway<ChapterArchivePageDto>, VolumeChapterGateway {
        private val semaphore = Semaphore(permits = 3)

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

                            // 1. Detect and Sync Volumes
                            val subFolders = rootChildren.filter { it.mimeType == DocumentsContract.Document.MIME_TYPE_DIR }
                            val volumeMap = mutableMapOf<String, Long>() // Path -> VolumeId

                            val existingVolumes = volumeArchiveDao.getVolumesListByDirectoryId(comicId)
                            val existingVolumesMap = existingVolumes.associateBy { it.path }
                            val volumesToDelete = existingVolumes.toMutableList()

                            subFolders.forEach { subFolder ->
                                val sortResult = SortNormalizer.normalize(subFolder.name, SortType.VOLUME)

                                val subFolderUri =
                                    if (baseUri != null) {
                                        DocumentsContract.buildDocumentUriUsingTree(baseUri, subFolder.id).toString()
                                    } else {
                                        DocumentsContract.buildDocumentUriUsingTree(folderUri, subFolder.id).toString()
                                    }

                                val existing = existingVolumesMap[subFolderUri]
                                if (existing != null) {
                                    volumesToDelete.remove(existing)
                                    volumeMap[subFolderUri] = existing.id

                                } else {
                                    // Detect volume media
                                    val subFolderChildren =
                                        if (baseUri != null) {
                                            ContentQueryHelper.listFiles(context, baseUri, subFolder.id).getOrElse { emptyList() }
                                        } else {
                                            DocumentFile.fromSingleUri(context, subFolderUri.toUri())?.listFiles()?.map { it.toFastMetadata() }
                                                ?: emptyList()
                                        }

                                    val cover =
                                        subFolderChildren.find { MediaFilePattern.isCover(it.name) }?.let {
                                            if (baseUri != null) {
                                                DocumentsContract.buildDocumentUriUsingTree(baseUri, it.id).toString()
                                            } else {
                                                DocumentsContract.buildDocumentUriUsingTree(subFolderUri.toUri(), it.id).toString()
                                            }
                                        }

                                    val banner =
                                        subFolderChildren.find { MediaFilePattern.isBanner(it.name) }?.let {
                                            if (baseUri != null) {
                                                DocumentsContract.buildDocumentUriUsingTree(baseUri, it.id).toString()
                                            } else {
                                                DocumentsContract.buildDocumentUriUsingTree(subFolderUri.toUri(), it.id).toString()
                                            }
                                        }

                                    val newVolume =
                                        subFolder.toVolumeArchiveEntity(
                                            comicId = comicId,
                                            volumeSort = sortResult.normalizedSort,
                                            folderUri = subFolderUri,
                                            isSpecial = sortResult.isSpecial,
                                            coverPath = cover,
                                            bannerPath = banner,
                                        )
                                    val newId = volumeArchiveDao.insert(newVolume)
                                    volumeMap[subFolderUri] = newId
                                }
                            }

                            volumesToDelete.forEach { volumeArchiveDao.delete(it) }

                            // 2. Determine Template (needed for PDF conversion check)
                            val allTemplates = templateService.getTemplates()
                            var activeTemplate = folder.chapterTemplateFk?.let { id -> allTemplates.find { it.id == id } }

                            val initialFilenames = mutableListOf<String>()
                            initialFilenames.addAll(rootChildren.map { it.name })
                            subFolders.forEach { subFolder ->
                                val subFolderChildren = listFilesMetadata(context, baseUri, folderUri, subFolder.id)
                                initialFilenames.addAll(subFolderChildren.map { it.name })
                            }

                            if (activeTemplate == null && initialFilenames.isNotEmpty()) {
                                AcerolaLogger.d(TAG, "Detecting best template for comic: ${folder.name}", LogSource.REPOSITORY)
                                activeTemplate =
                                    findBestTemplate(
                                        initialFilenames,
                                        allTemplates,
                                    ) ?: allTemplates.find { it.id == -2L } ?: allTemplates.firstOrNull()
                                if (activeTemplate != null) directoryDao.update(folder.copy(chapterTemplateFk = activeTemplate.id))
                            }

                            val defaultPattern = ChapterTemplatePattern.presets.values.first()
                            val chapterRegex = templateToRegex(template = activeTemplate?.pattern ?: defaultPattern)

                            // 3. Hierarchical PDF to CBZ conversion
                            val foldersToProcess = mutableListOf<Pair<DocumentFile, List<FastFileMetadata>>>()
                            foldersToProcess.add(folderDoc to rootChildren)

                            subFolders.forEach { subFolder ->
                                // Navigation via findFile ensures we get a TreeDocumentFile that supports createFile
                                val subDoc = folderDoc.findFile(subFolder.name) ?: return@forEach
                                val subChildren = listFilesMetadata(context, baseUri, folderUri, subFolder.id)
                                foldersToProcess.add(subDoc to subChildren)
                            }

                            var needsGlobalRefresh = false
                            foldersToProcess.forEach { (dir, children) ->
                                val pdfFiles = children.filter { it.name.endsWith(ArchiveFormatPattern.PDF.extension, ignoreCase = true) }
                                if (pdfFiles.isEmpty()) return@forEach

                                AcerolaLogger.d(TAG, "Checking ${pdfFiles.size} PDF files in folder: ${dir.name}", LogSource.REPOSITORY)
                                val cbzNames = children.map { it.name }.toSet()

                                pdfFiles.forEach { pdf ->
                                    val targetCbzName = pdf.name.substringBeforeLast('.') + ArchiveFormatPattern.CBZ.extension

                                    if (cbzNames.contains(targetCbzName)) {
                                        AcerolaLogger.v(TAG, "Skipping PDF conversion: $targetCbzName already exists", LogSource.REPOSITORY)
                                        return@forEach
                                    }

                                    if (!chapterRegex.matches(targetCbzName)) {
                                        AcerolaLogger.v(
                                            TAG,
                                            "Skipping PDF conversion: ${pdf.name} does not match chapter pattern",
                                            LogSource.REPOSITORY,
                                        )
                                        return@forEach
                                    }

                                    AcerolaLogger.i(TAG, "Starting conversion: ${pdf.name} -> $targetCbzName in ${dir.name}", LogSource.REPOSITORY)
                                    val pdfDocUri =
                                        if (baseUri != null) {
                                            DocumentsContract.buildDocumentUriUsingTree(baseUri, pdf.id)
                                        } else {
                                            pdf.id.toUri()
                                        }

                                    val pdfDoc = DocumentFile.fromSingleUri(context, pdfDocUri)
                                    if (pdfDoc == null) {
                                        AcerolaLogger.e(TAG, "Failed to open PDF document: ${pdf.name}", LogSource.REPOSITORY)
                                        return@forEach
                                    }

                                    pdfToCbzConverterService
                                        .convertPdfToCbz(dir, pdfDoc, targetCbzName)
                                        .onRight {
                                            AcerolaLogger.i(TAG, "Successfully converted: ${pdf.name} -> $targetCbzName", LogSource.REPOSITORY)
                                            needsGlobalRefresh = true
                                        }.onLeft { error ->
                                            AcerolaLogger.e(TAG, "Failed to convert PDF ${pdf.name}: $error", LogSource.REPOSITORY)
                                        }
                                }
                            }

                            // 4. Collect all files (with refresh if needed)
                            val finalRootChildren =
                                if (needsGlobalRefresh && baseUri != null) {
                                    AcerolaLogger.d(TAG, "Files converted, refreshing root file list", LogSource.REPOSITORY)
                                    val folderDocId = DocumentsContract.getDocumentId(folderUri)
                                    ContentQueryHelper.listFiles(context, baseUri, folderDocId).getOrElse { rootChildren }
                                } else {
                                    rootChildren
                                }

                            val allChapterFiles = mutableListOf<Pair<FastFileMetadata, Long?>>() // File -> VolumeId?

                            // Root chapters
                            finalRootChildren.filter { it.isFile && ArchiveFormatPattern.isIndexable(it.name) }.forEach {
                                allChapterFiles.add(it to null)
                            }

                            // Volume chapters
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
                                        listFilesMetadata(context, baseUri, folderUri, subFolder.id)
                                    }

                                subFolderChildren.filter { it.isFile && ArchiveFormatPattern.isIndexable(it.name) }.forEach {
                                    allChapterFiles.add(it to volId)
                                }
                            }

                            // 5. Process Chapters
                            val existingChapters = chapterArchiveDao.getChaptersListByDirectoryId(folderId = comicId)
                            val existingChaptersMap = existingChapters.associateBy { it.path }
                            val chaptersToInsert = mutableListOf<ChapterArchive>()
                            val chaptersToDelete = existingChapters.toMutableList()

                            val processedSorts = mutableSetOf<String>()

                            allChapterFiles.forEachIndexed { index, (file, volumeId) ->
                                val name = file.name
                                val sortResult = SortNormalizer.normalize(name, SortType.CHAPTER)

                                // Unique check per comic
                                if (processedSorts.contains(sortResult.normalizedSort)) {
                                    AcerolaLogger.e(
                                        TAG,
                                        "Duplicate chapter detected: ${sortResult.normalizedSort} in ${folder.name}. Skipping.",
                                        LogSource.REPOSITORY,
                                    )
                                    return@forEachIndexed
                                }
                                processedSorts.add(sortResult.normalizedSort)

                                val currentFastHash = "${file.name}|${file.size}|${file.lastModified}"
                                val fileUri =
                                    if (baseUri != null) {
                                        DocumentsContract.buildDocumentUriUsingTree(baseUri, file.id).toString()
                                    } else {
                                        DocumentsContract.buildDocumentUriUsingTree(folderUri, file.id).toString()
                                    }

                                val existing = existingChaptersMap[fileUri]
                                if (existing != null && existing.fastHash == currentFastHash && existing.volumeIdFk == volumeId) {
                                    chaptersToDelete.remove(existing)
                                    return@forEachIndexed
                                }

                                semaphore.withPermit {
                                    chaptersToInsert.add(
                                        file.toChapterArchiveEntity(
                                            comicId = comicId,
                                            fileUri = fileUri,
                                            chapterSort = sortResult.normalizedSort,
                                            fastHash = currentFastHash,
                                            volumeIdFk = volumeId,
                                            isSpecial = sortResult.isSpecial,
                                        ),
                                    )
                                }
                                _progress.value = 30 + ((index + 1) * 60 / allChapterFiles.size)
                            }

                            if (chaptersToDelete.isNotEmpty()) {
                                AcerolaLogger.d(TAG, "Deleting ${chaptersToDelete.size} stale chapters", LogSource.REPOSITORY)
                                chaptersToDelete.forEach { chapterArchiveDao.delete(it) }
                            }

                            if (chaptersToInsert.isNotEmpty()) {
                                AcerolaLogger.d(TAG, "Inserting ${chaptersToInsert.size} new chapters", LogSource.REPOSITORY)
                                chapterArchiveDao.insertAll(*chaptersToInsert.toTypedArray())
                            }

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

        private fun findBestTemplate(
            filenames: List<String>,
            templates: List<ChapterTemplate>,
        ): ChapterTemplate? {
            if (filenames.isEmpty()) return null

            val counts =
                templates.associateWith { template ->
                    val regex = templateToRegex(template.pattern)
                    filenames.count { filename ->
                        regex.matches(filename) ||
                            regex.matches(
                                filename.substringBeforeLast(".") + ArchiveFormatPattern.CBZ.extension,
                            )
                    }
                }

            return counts.entries
                .filter { it.value > 0 }
                .sortedWith(
                    compareByDescending<Map.Entry<ChapterTemplate, Int>> { it.value }
                        .thenByDescending { it.key.id > 0 }
                        .thenByDescending { it.key.id },
                ).firstOrNull()
                ?.key
        }

        override fun observeChapters(
            comicId: Long,
            sortType: String,
            isAscending: Boolean,
        ): StateFlow<ChapterArchivePageDto> =
            chapterArchiveDao
                .getChaptersByDirectoryId(folderId = comicId)
                .map { list ->
                    AcerolaLogger.d(TAG, "Observed chapter list update: ${list.size} chapters", LogSource.REPOSITORY)
                    val baseList =
                        if (sortType == "LAST_UPDATE") {
                            list.sortedBy { it.chapter.lastModified }
                        } else {
                            list
                        }

                    val finalList = if (isAscending) baseList else baseList.reversed()
                    finalList.toViewPageDto()
                }.stateIn(
                    started = SharingStarted.Lazily,
                    scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
                    initialValue = ChapterArchivePageDto(items = emptyList(), pageSize = 0, total = 0, page = 0),
                )

        override fun observeVolumeGroups(
            comicId: Long,
            previewSize: Int,
            sortType: String,
            isAscending: Boolean,
        ): StateFlow<List<VolumeChapterGroupDto>> =
            volumeArchiveDao
                .getVolumeChapterCountsByDirectoryId(comicId)
                .map { summaries ->
                    val sortedSummaries = if (isAscending) summaries else summaries.reversed()
                    sortedSummaries.map { summary ->
                        val previewItems =
                            chapterArchiveDao
                                .getChaptersByVolumePaged(
                                    comicId = comicId,
                                    volumeId = summary.id,
                                    pageSize = previewSize,
                                    offset = 0,
                                ).let { joins ->
                                    val base = joins.map { it.toViewDto() }
                                    if (sortType == "LAST_UPDATE") {
                                        val ordered = base.sortedBy { it.lastModified }
                                        if (isAscending) ordered else ordered.reversed()
                                    } else {
                                        if (isAscending) base else base.reversed()
                                    }
                                }

                        summary.toGroupDto(items = previewItems)
                    }
                }.stateIn(
                    started = SharingStarted.Lazily,
                    scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
                    initialValue = emptyList(),
                )

        override suspend fun getVolumeChapterPage(
            comicId: Long,
            volumeId: Long,
            offset: Int,
            pageSize: Int,
            sortType: String,
            isAscending: Boolean,
        ): List<ChapterFileDto> =
            chapterArchiveDao
                .getChaptersByVolumePaged(
                    comicId = comicId,
                    volumeId = volumeId,
                    pageSize = pageSize,
                    offset = offset,
                ).let { joins ->
                    val base = joins.map { it.toViewDto() }
                    if (sortType == "LAST_UPDATE") {
                        val ordered = base.sortedBy { it.lastModified }
                        if (isAscending) ordered else ordered.reversed()
                    } else {
                        if (isAscending) base else base.reversed()
                    }
                }

        override fun observeHasRootChapters(comicId: Long): Flow<Boolean> =
            chapterArchiveDao.observeRootChaptersCountByDirectoryId(comicId).map { it > 0 }

        override suspend fun getChapterPage(
            comicId: Long,
            total: Int,
            page: Int,
            pageSize: Int,
            sortType: String,
            isAscending: Boolean,
        ): ChapterArchivePageDto {
            AcerolaLogger.d(TAG, "Retrieving chapter page: $page (pageSize: $pageSize, sort: $sortType, asc: $isAscending)", LogSource.REPOSITORY)

            // Por enquanto, se a ordenação for NUMBER ASC, podemos usar o método Paged do banco de dados diretamente.
            // Se for qualquer outra coisa, precisamos buscar todos os registros, ordenar/inverter e, em seguida, paginar.
            // Isso ainda é melhor do que fazer no ViewModel, pois centraliza a lógica.
            return if (sortType == "NUMBER" && isAscending) {
                val offset = page * pageSize
                val realTotal = if (total > 0) total else chapterArchiveDao.countByDirectoryId(folderId = comicId)
                val items =
                    chapterArchiveDao.getChaptersByDirectoryPaged(
                        pageSize = pageSize,
                        folderId = comicId,
                        offset = offset,
                    )

                items.toViewPageDto(pageSize = pageSize, total = realTotal, page = page)
            } else {
                // NOTE: getChaptersListByDirectoryId não faz a junção com o volume; precisamos de uma versão com junção se quisermos ordenação hierárquica aqui também.
                // Para simplificar, vamos usar a lógica da versão do Flow ou adicionar um novo método Dao.
                // Mas observeChapters já lida com isso.
                val flowList = chapterArchiveDao.getChaptersByDirectoryId(comicId).first()

                val baseList =
                    if (sortType == "LAST_UPDATE") {
                        flowList.sortedBy { it.chapter.lastModified }
                    } else {
                        flowList
                    }

                val sortedList = if (isAscending) baseList else baseList.reversed()

                val realTotal = sortedList.size
                val start = (page * pageSize).coerceIn(0, realTotal)
                val end = (start + pageSize).coerceIn(0, realTotal)
                val pagedList = if (start < realTotal) sortedList.subList(start, end) else emptyList()

                pagedList.toViewPageDto(pageSize = pageSize, total = realTotal, page = page)
            }
        }

        fun observeAllChapterCounts(): Flow<Map<Long, Int>> =
            chapterArchiveDao.getChapterCountsByDirectory().map { list ->
                list.associate { it.comicDirectoryFk to it.count }
            }

        private fun listFilesMetadata(
            context: Context,
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
