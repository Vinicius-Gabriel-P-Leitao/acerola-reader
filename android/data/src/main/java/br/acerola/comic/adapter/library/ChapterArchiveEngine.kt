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
import br.acerola.comic.dto.archive.ChapterArchivePageDto
import br.acerola.comic.error.message.LibrarySyncError
import br.acerola.comic.local.dao.archive.ChapterArchiveDao
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.local.entity.archive.ChapterArchive
import br.acerola.comic.local.entity.archive.ChapterTemplate
import br.acerola.comic.local.translator.persistence.toChapterArchiveEntity
import br.acerola.comic.local.translator.ui.toViewPageDto
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.pattern.ArchiveFormatPattern
import br.acerola.comic.pattern.ChapterTemplatePattern
import br.acerola.comic.service.compact.PdfToCbzConverter
import br.acerola.comic.service.template.ChapterNameProcessor
import br.acerola.comic.util.ContentQueryHelper
import br.acerola.comic.util.FastFileMetadata
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterArchiveEngine @Inject constructor(
    private val directoryDao: ComicDirectoryDao,
    private val chapterArchiveDao: ChapterArchiveDao,
    private val templateService: ChapterNameProcessor,
    @param:ApplicationContext private val context: Context,
    private val pdfToCbzConverterService: PdfToCbzConverter,
) : ChapterGateway<ChapterArchivePageDto> {

    private val semaphore = Semaphore(permits = 3)

    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    override suspend fun refreshComicChapters(mangaId: Long, baseUri: Uri?): Either<LibrarySyncError, Unit> =
        withContext(context = Dispatchers.IO) {
            AcerolaLogger.i(TAG, "Starting chapter sync for mangaId: $mangaId", LogSource.REPOSITORY)
            _isIndexing.value = true
            _progress.value = 0

            val result = Either.catch {
                val folder = directoryDao.getMangaDirectoryById(mangaId = mangaId) ?: return@catch
                val folderUri = folder.path.toUri()

                var allFiles: List<FastFileMetadata>
                val folderDoc: DocumentFile

                if (baseUri != null) {
                    val folderDocId = DocumentsContract.getDocumentId(folderUri)
                    allFiles = ContentQueryHelper.listFiles(context, baseUri, folderDocId).getOrElse { return@catch }
                    folderDoc = DocumentFile.fromTreeUri(context, folderUri) ?: return@catch
                } else {
                    folderDoc = DocumentFile.fromSingleUri(context, folderUri) ?: return@catch
                    allFiles = folderDoc.listFiles().filter { it.isFile }.map {
                        it.toFastMetadata()
                    }
                }

                val allTemplates = templateService.getTemplates()
                var activeTemplate = folder.chapterTemplateFk?.let { id ->
                    allTemplates.find { it.id == id }
                }

                if (activeTemplate == null && allFiles.isNotEmpty()) {
                    val filenames = allFiles.map { it.name }
                    activeTemplate = findBestTemplate(filenames, allTemplates) ?: allTemplates.find { it.id == -2L } ?: allTemplates.firstOrNull()

                    if (activeTemplate != null) {
                        directoryDao.update(folder.copy(chapterTemplateFk = activeTemplate.id))
                    }
                }

                val defaultPattern = ChapterTemplatePattern.presets.values.first()
                val chapterRegex = templateToRegex(template = activeTemplate?.pattern ?: defaultPattern)

                val existingChapters = chapterArchiveDao.getChaptersByComicDirectoryList(folderId = mangaId)
                val existingChaptersMap = existingChapters.associateBy { it.path }
                val folderLastModified = if (baseUri == null) folderDoc.lastModified() else 0

                // NOTE: Processa PDFs to CBZ
                val pdfFiles = allFiles.filter { it.name.endsWith(ArchiveFormatPattern.PDF.extension, ignoreCase = true) }
                val cbzNames = allFiles.map { it.name }.toSet()
                
                if (pdfFiles.isNotEmpty()) {
                    var needsRefresh = false
                    pdfFiles.forEach { pdf ->
                        val fakeCbzName = pdf.name.substringBeforeLast(".") + ArchiveFormatPattern.CBZ.extension
                        val match = chapterRegex.matchEntire(fakeCbzName)

                        if (match != null) {
                            val targetCbzName = pdf.name.substringBeforeLast('.') + ArchiveFormatPattern.CBZ.extension
                            if (!cbzNames.contains(targetCbzName)) {
                                AcerolaLogger.i(TAG, "Converting PDF to CBZ: ${pdf.name} -> $targetCbzName", LogSource.REPOSITORY)

                                val pdfDocUri = if (baseUri != null) {
                                    DocumentsContract.buildDocumentUriUsingTree(baseUri, pdf.id)
                                } else {
                                    DocumentsContract.buildDocumentUriUsingTree(folderUri, pdf.id)
                                }

                                val pdfDoc = DocumentFile.fromSingleUri(context, pdfDocUri)
                                if (pdfDoc != null) {
                                    pdfToCbzConverterService.convertPdfToCbz(folderDoc, pdfDoc, targetCbzName)
                                    needsRefresh = true
                                }
                            }
                        }
                    }
                    
                    if (needsRefresh) {
                        if (baseUri != null) {
                            val folderDocId = DocumentsContract.getDocumentId(folderUri)
                            allFiles = ContentQueryHelper.listFiles(context, baseUri, folderDocId).getOrElse { return@catch }
                        } else {
                            allFiles = folderDoc.listFiles().filter { it.isFile }.map {
                                it.toFastMetadata()
                            }
                        }
                    }
                }

                val chapterFiles = allFiles.filter { ArchiveFormatPattern.isIndexable(ext = it.name) }

                if (existingChapters.isNotEmpty() && baseUri == null && folder.lastModified >= folderLastModified) {
                    AcerolaLogger.d(TAG, "No changes detected for comic: ${folder.name}, skipping sync", LogSource.REPOSITORY)
                    return@catch
                }

                _progress.value = 30

                val chaptersToInsert = mutableListOf<ChapterArchive>()
                val chaptersToDelete = existingChapters.toMutableList()

                chapterFiles.forEachIndexed { index, file ->
                    val name = file.name
                    val match = chapterRegex.matchEntire(input = name)

                    val currentFastHash = "${file.name}|${file.size}|${file.lastModified}"

                    val fileUri = if (baseUri != null) {
                        DocumentsContract.buildDocumentUriUsingTree(baseUri, file.id).toString()
                    } else {
                        DocumentsContract.buildDocumentUriUsingTree(folderUri, file.id).toString()
                    }

                    val existing = existingChaptersMap[fileUri]

                    if (existing != null && existing.fastHash == currentFastHash) {
                        chaptersToDelete.remove(existing)
                        return@forEachIndexed
                    }

                    semaphore.withPermit {
                        val chapterSort: String

                        if (match != null) {
                            val integerPart = match.groupValues[1].toInt()
                            val fractionalPartRaw = match.groupValues.getOrNull(index = 2)
                            val fractionalPart = fractionalPartRaw?.toIntOrNull() ?: 0

                            chapterSort = if (fractionalPart == 0) integerPart.toString()
                            else "$integerPart.$fractionalPart"
                        } else {
                            chapterSort = name.filter { it.isDigit() || it == '.' || it == ',' }
                                .replace(',', '.')
                                .ifBlank { (index + 1).toString() }
                        }

                        chaptersToInsert.add(
                            file.toChapterArchiveEntity(
                                mangaId = mangaId,
                                fileUri = fileUri,
                                chapterSort = chapterSort,
                                fastHash = currentFastHash
                            )
                        )
                    }

                    _progress.value = 30 + ((index + 1) * 60 / chapterFiles.size)
                }

                if (chaptersToDelete.isNotEmpty()) {
                    AcerolaLogger.d(TAG, "Deleting ${chaptersToDelete.size} missing chapters for comic: ${folder.name}", LogSource.REPOSITORY)
                    chaptersToDelete.forEach { chapterArchiveDao.delete(it) }
                }

                if (chaptersToInsert.isNotEmpty()) {
                    AcerolaLogger.d(TAG, "Inserting ${chaptersToInsert.size} new chapters for comic: ${folder.name}", LogSource.REPOSITORY)
                    chapterArchiveDao.insertAll(*chaptersToInsert.toTypedArray())
                }

                if (folderLastModified > 0 && folder.lastModified < folderLastModified) {
                    directoryDao.update(entity = folder.copy(lastModified = folderLastModified))
                }

                AcerolaLogger.i(TAG, "Finished chapter sync for comic: ${folder.name}", LogSource.REPOSITORY)
                _progress.value = 100
            }.mapLeft { exception ->
                AcerolaLogger.e(TAG, "Chapter sync failed for mangaId: $mangaId", LogSource.REPOSITORY, throwable = exception)
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

    private fun findBestTemplate(filenames: List<String>, templates: List<ChapterTemplate>): ChapterTemplate? {
        if (filenames.isEmpty()) return null

        val counts = templates.associateWith { template ->
            val regex = templateToRegex(template.pattern)
            filenames.count { filename ->
                regex.matches(filename) || regex.matches(
                    filename.substringBeforeLast(".") + ArchiveFormatPattern.CBZ.extension
                )
            }
        }

        return counts.entries
            .filter { it.value > 0 }
            .sortedWith(
                compareByDescending<Map.Entry<ChapterTemplate, Int>> { it.value }
                    .thenByDescending { it.key.id > 0 }
                    .thenByDescending { it.key.id }
            )
            .firstOrNull()?.key
    }

    override fun observeChapters(mangaId: Long): StateFlow<ChapterArchivePageDto> {
        return chapterArchiveDao.getChaptersByComicDirectory(folderId = mangaId).map { list: List<ChapterArchive> ->
            AcerolaLogger.d(TAG, "Observed chapter list update: ${list.size} chapters", LogSource.REPOSITORY)
            list.toViewPageDto()
        }.stateIn(
            started = SharingStarted.Lazily,
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            initialValue = ChapterArchivePageDto(items = emptyList(), pageSize = 0, total = 0, page = 0)
        )
    }

    override suspend fun getChapterPage(mangaId: Long, total: Int, page: Int, pageSize: Int): ChapterArchivePageDto {
        val offset = page * pageSize
        AcerolaLogger.d(TAG, "Retrieving chapter page: $page (pageSize: $pageSize)", LogSource.REPOSITORY)

        val realTotal = if (total > 0) total
        else chapterArchiveDao.countChaptersByComicDirectory(folderId = mangaId)

        val items = chapterArchiveDao.getChaptersPaged(
            pageSize = pageSize, folderId = mangaId, offset = offset
        )

        return items.toViewPageDto(pageSize = pageSize, total = realTotal, page = page)
    }

    override fun observeSpecificChapters(
        mangaId: Long,
        chapters: List<String>
    ): Flow<ChapterArchivePageDto> {
        return chapterArchiveDao.getChaptersByComicAndSorts(folderId = mangaId, chapters = chapters)
            .map { list ->
                list.toViewPageDto()
            }
    }

    fun observeAllChapterCounts(): Flow<Map<Long, Int>> {
        return chapterArchiveDao.getAllChapterCounts().map { list ->
            list.associate { it.comic_directory_fk to it.count }
        }
    }

    companion object {
        private const val TAG = "ChapterArchiveRepository"
    }
}
