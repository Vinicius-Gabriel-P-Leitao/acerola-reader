package br.acerola.manga.repository.adapter.local.chapter

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.manga.config.preference.FileExtension
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.database.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.entity.archive.ChapterArchive
import br.acerola.manga.local.mapper.toChapterArchiveModel
import br.acerola.manga.local.mapper.toPageDto
import br.acerola.manga.repository.port.ChapterManagementRepository
import br.acerola.manga.util.sha256
import br.acerola.manga.util.templateToRegex
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    override suspend fun refreshMangaChapters(mangaId: Long): Either<LibrarySyncError, Unit> =
        withContext(context = Dispatchers.IO) {
            _isIndexing.value = true
            _progress.value = 0

            Either.catch {
                val folder = directoryDao.getMangaDirectoryById(mangaId = mangaId) ?: return@catch
                val folderDoc =
                    DocumentFile.fromTreeUri(context, folder.path.toUri()) ?: return@catch

                // NOTE: Isso aqui vai existir só quando eu quiser pegar os que não existem no DB
                val chaptersExist =
                    chapterArchiveDao.countChaptersByMangaDirectory(folderId = mangaId) > 0

                if (chaptersExist && folder.lastModified >= folderDoc.lastModified()) {
                    return@catch
                }

                _progress.value = 10

                val chapterFiles = folderDoc.listFiles().filter { it.isFile }.filter { file ->
                    FileExtension.isSupported(ext = file.name)
                }

                _progress.value = 30

                chapterArchiveDao.deleteChaptersByMangaDirectoryId(folderId = mangaId)

                val chapterRegex =
                    templateToRegex(template = folder.chapterTemplate ?: "{value}{sub}.*.cbz")

                val chapters = chapterFiles.mapIndexedNotNull { index, file ->
                    val name = file.name ?: return@mapIndexedNotNull null
                    val match = chapterRegex.matchEntire(input = name)
                        ?: return@mapIndexedNotNull null

                    val integerPart = match.groupValues[1].toInt()

                    val fractionalPartRaw = match.groupValues.getOrNull(index = 2)
                    val fractionalPart = fractionalPartRaw?.toIntOrNull() ?: 0

                    val chapterSort = if (fractionalPart == 0) integerPart.toString()
                    else "$integerPart.$fractionalPart"

                    val currentProgress = 30 + ((index + 1) * 60 / chapterFiles.size)
                    _progress.value = currentProgress

                    file.toChapterArchiveModel(
                        mangaId = mangaId,
                        chapterSort = chapterSort,
                        checksum = file.sha256(context)
                    )
                }

                if (chapters.isNotEmpty()) {
                    chapterArchiveDao.insertAll(*chapters.toTypedArray())
                }

                if (folder.lastModified < folderDoc.lastModified()) {
                    directoryDao.update(entity = folder.copy(lastModified = folderDoc.lastModified()))
                }

                _progress.value = 100
            }.mapLeft { exception ->
                when (exception) {
                    is SecurityException -> LibrarySyncError.FolderAccessDenied(cause = exception)
                    is IOException -> LibrarySyncError.DiskIOFailure(
                        path = "Unknown",
                        cause = exception
                    )

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
            list.toPageDto()
        }.stateIn(
            started = SharingStarted.Lazily,
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            initialValue = ChapterArchivePageDto(items = emptyList(), pageSize = 0, total = 0, page = 0)
        )
    }

    override suspend fun getChapterPage(mangaId: Long, total: Int, page: Int, pageSize: Int): ChapterArchivePageDto {
        val offset = page * pageSize

        val realTotal = if (total > 0) {
            total
        } else {
            chapterArchiveDao.countChaptersByMangaDirectory(folderId = mangaId)
        }

        val items = chapterArchiveDao.getChaptersPaged(
            pageSize = pageSize, folderId = mangaId, offset = offset
        )

        return items.toPageDto(pageSize = pageSize, total = realTotal, page = page)
    }

    override fun observeSpecificChapters(
        mangaId: Long,
        chapters: List<String>
    ): kotlinx.coroutines.flow.Flow<ChapterArchivePageDto> {
        return chapterArchiveDao.getChaptersByMangaAndSorts(folderId = mangaId, chapters = chapters)
            .map { list ->
                list.toPageDto()
            }
    }
}
