package br.acerola.manga.adapter.library

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import arrow.core.getOrElse
import br.acerola.manga.adapter.contract.ChapterPort
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.entity.archive.ChapterArchive
import br.acerola.manga.local.translator.toPageDto
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.pattern.ArchiveFormatPattern
import br.acerola.manga.service.compact.PdfToCbzConverterService
import br.acerola.manga.util.ContentQueryHelper
import br.acerola.manga.util.FastFileMetadata
import br.acerola.manga.util.templateToRegex
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
    private val directoryDao: MangaDirectoryDao,
    private val chapterArchiveDao: ChapterArchiveDao,
    private val pdfToCbzConverterService: PdfToCbzConverterService,
    @param:ApplicationContext private val context: Context
) : ChapterPort<ChapterArchivePageDto> {

    private val semaphore = Semaphore(permits = 3)

    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    override suspend fun refreshMangaChapters(mangaId: Long, baseUri: Uri?): Either<LibrarySyncError, Unit> =
        withContext(context = Dispatchers.IO) {
            AcerolaLogger.i(TAG, "Starting chapter sync for mangaId: $mangaId", LogSource.REPOSITORY)
            _isIndexing.value = true
            _progress.value = 0

            Either.catch {
                val folder = directoryDao.getMangaDirectoryById(mangaId = mangaId) ?: return@catch
                val folderUri = folder.path.toUri()
                val chapterRegex = templateToRegex(template = folder.chapterTemplate ?: "{value}{sub}.*.{extension}")

                val existingChapters = chapterArchiveDao.getChaptersByMangaDirectoryList(folderId = mangaId)
                val existingChaptersMap = existingChapters.associateBy { it.path }

                var allFiles: List<FastFileMetadata>
                val folderLastModified: Long
                val folderDoc: DocumentFile

                if (baseUri != null) {
                    val folderDocId = DocumentsContract.getDocumentId(folderUri)
                    allFiles = ContentQueryHelper.listFiles(context, baseUri, folderDocId).getOrElse { return@catch }
                    folderLastModified = 0
                    folderDoc = DocumentFile.fromTreeUri(context, folderUri) ?: return@catch
                } else {
                    folderDoc = DocumentFile.fromSingleUri(context, folderUri) ?: return@catch
                    folderLastModified = folderDoc.lastModified()
                    allFiles = folderDoc.listFiles().filter { it.isFile }.map {
                        FastFileMetadata(
                            id = DocumentsContract.getDocumentId(it.uri),
                            name = it.name ?: "",
                            size = it.length(),
                            lastModified = it.lastModified(),
                            mimeType = ""
                        )
                    }
                }

                // Process PDFs to CBZ
                val pdfFiles = allFiles.filter { it.name.endsWith(".pdf", ignoreCase = true) }
                val cbzNames = allFiles.map { it.name }.toSet()
                
                if (pdfFiles.isNotEmpty()) {
                    var needsRefresh = false
                    pdfFiles.forEach { pdf ->
                        val match = chapterRegex.matchEntire(pdf.name)
                        if (match != null) {
                            val targetCbzName = pdf.name.substringBeforeLast('.') + ".cbz"
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
                        // Re-fetch
                        if (baseUri != null) {
                            val folderDocId = DocumentsContract.getDocumentId(folderUri)
                            allFiles = ContentQueryHelper.listFiles(context, baseUri, folderDocId).getOrElse { return@catch }
                        } else {
                            allFiles = folderDoc.listFiles().filter { it.isFile }.map {
                                FastFileMetadata(
                                    id = DocumentsContract.getDocumentId(it.uri),
                                    name = it.name ?: "",
                                    size = it.length(),
                                    lastModified = it.lastModified(),
                                    mimeType = ""
                                )
                            }
                        }
                    }
                }

                val chapterFiles = allFiles.filter { ArchiveFormatPattern.isIndexable(ext = it.name) }

                if (existingChapters.isNotEmpty() && baseUri == null && folder.lastModified >= folderLastModified) {
                    AcerolaLogger.d(TAG, "No changes detected for manga: ${folder.name}, skipping sync", LogSource.REPOSITORY)
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
                            ChapterArchive(
                                chapter = name,
                                path = fileUri,
                                checksum = null,
                                fastHash = currentFastHash,
                                chapterSort = chapterSort,
                                folderPathFk = mangaId
                            )
                        )
                    }

                    _progress.value = 30 + ((index + 1) * 60 / chapterFiles.size)
                }

                if (chaptersToDelete.isNotEmpty()) {
                    AcerolaLogger.d(TAG, "Deleting ${chaptersToDelete.size} missing chapters for manga: ${folder.name}", LogSource.REPOSITORY)
                    chaptersToDelete.forEach { chapterArchiveDao.delete(it) }
                }

                if (chaptersToInsert.isNotEmpty()) {
                    AcerolaLogger.d(TAG, "Inserting ${chaptersToInsert.size} new chapters for manga: ${folder.name}", LogSource.REPOSITORY)
                    chapterArchiveDao.insertAll(*chaptersToInsert.toTypedArray())
                }

                if (folderLastModified > 0 && folder.lastModified < folderLastModified) {
                    directoryDao.update(entity = folder.copy(lastModified = folderLastModified))
                }

                AcerolaLogger.i(TAG, "Finished chapter sync for manga: ${folder.name}", LogSource.REPOSITORY)
                _progress.value = 100
            }.mapLeft { exception ->
                AcerolaLogger.e(TAG, "Chapter sync failed for mangaId: $mangaId", LogSource.REPOSITORY, throwable = exception)
                when (exception) {
                    is IOException -> LibrarySyncError.DiskIOFailure(path = "Unknown", cause = exception)
                    is SecurityException -> LibrarySyncError.FolderAccessDenied(cause = exception)
                    is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                    else -> LibrarySyncError.UnexpectedError(cause = exception)
                }
            }.also {
                _isIndexing.value = false
                _progress.value = -1
            }
        }

    override fun observeChapters(mangaId: Long): StateFlow<ChapterArchivePageDto> {
        return chapterArchiveDao.getChaptersByMangaDirectory(folderId = mangaId).map { list: List<ChapterArchive> ->
            AcerolaLogger.d(TAG, "Observed chapter list update: ${list.size} chapters", LogSource.REPOSITORY)
            list.toPageDto()
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
        else chapterArchiveDao.countChaptersByMangaDirectory(folderId = mangaId)

        val items = chapterArchiveDao.getChaptersPaged(
            pageSize = pageSize, folderId = mangaId, offset = offset
        )

        return items.toPageDto(pageSize = pageSize, total = realTotal, page = page)
    }

    override fun observeSpecificChapters(
        mangaId: Long,
        chapters: List<String>
    ): Flow<ChapterArchivePageDto> {
        return chapterArchiveDao.getChaptersByMangaAndSorts(folderId = mangaId, chapters = chapters)
            .map { list ->
                list.toPageDto()
            }
    }

    fun observeAllChapterCounts(): Flow<Map<Long, Int>> {
        return chapterArchiveDao.getAllChapterCounts().map { list ->
            list.associate { it.manga_directory_fk to it.count }
        }
    }

    companion object {
        private const val TAG = "ChapterArchiveRepository"
    }
}
