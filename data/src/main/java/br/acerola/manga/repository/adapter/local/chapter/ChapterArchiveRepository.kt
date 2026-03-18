package br.acerola.manga.repository.adapter.local.chapter

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.manga.config.pattern.ArchiveFormatPattern
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.local.database.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.entity.archive.ChapterArchive
import br.acerola.manga.local.mapper.toPageDto
import br.acerola.manga.repository.port.ChapterManagementRepository
import android.provider.DocumentsContract
import br.acerola.manga.util.ContentQueryHelper
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
class ChapterArchiveRepository @Inject constructor(
    private val directoryDao: MangaDirectoryDao,
    private val chapterArchiveDao: ChapterArchiveDao,
    @param:ApplicationContext private val context: Context
) : ChapterManagementRepository<ChapterArchivePageDto> {

    private val semaphore = Semaphore(permits = 3)

    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    override suspend fun refreshMangaChapters(mangaId: Long, baseUri: android.net.Uri?): Either<LibrarySyncError, Unit> =
        withContext(context = Dispatchers.IO) {
            AcerolaLogger.i(TAG, "Starting chapter sync for mangaId: $mangaId", LogSource.REPOSITORY)  
            _isIndexing.value = true
            _progress.value = 0

            Either.catch {
                val folder = directoryDao.getMangaDirectoryById(mangaId = mangaId) ?: return@catch
                val folderUri = folder.path.toUri()
                
                val existingChapters = chapterArchiveDao.getChaptersByMangaDirectoryList(folderId = mangaId)
                val existingChaptersMap = existingChapters.associateBy { it.path }

                val chapterFiles: List<br.acerola.manga.util.FastFileMetadata>
                val folderLastModified: Long

                if (baseUri != null) {
                    val folderDocId = DocumentsContract.getDocumentId(folderUri)
                    chapterFiles = ContentQueryHelper.listFiles(context, baseUri, folderDocId).filter { 
                        ArchiveFormatPattern.isSupported(ext = it.name)
                    }
                    folderLastModified = 0 
                } else {
                    val folderDoc = DocumentFile.fromSingleUri(context, folderUri) ?: return@catch
                    folderLastModified = folderDoc.lastModified()
                    chapterFiles = folderDoc.listFiles().filter { it.isFile }.map { 
                        br.acerola.manga.util.FastFileMetadata(
                            id = DocumentsContract.getDocumentId(it.uri),
                            name = it.name ?: "",
                            size = it.length(),
                            lastModified = it.lastModified(),
                            mimeType = ""
                        )
                    }.filter { ArchiveFormatPattern.isSupported(ext = it.name) }
                }

                if (existingChapters.isNotEmpty() && baseUri == null && folder.lastModified >= folderLastModified) {
                    AcerolaLogger.d(TAG, "No changes detected for manga: ${folder.name}, skipping sync", LogSource.REPOSITORY)  
                    return@catch
                }

                _progress.value = 30

                val chapterRegex = templateToRegex(template = folder.chapterTemplate ?: "{value}{sub}.*.cbz")

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

        val realTotal = if (total > 0) {
            total
        } else chapterArchiveDao.countChaptersByMangaDirectory(folderId = mangaId)

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

    companion object {
        private const val TAG = "ChapterArchiveRepository"  
    }
}
