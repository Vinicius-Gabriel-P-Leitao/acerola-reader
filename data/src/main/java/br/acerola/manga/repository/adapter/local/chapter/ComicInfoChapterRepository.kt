package br.acerola.manga.repository.adapter.local.chapter

import android.database.sqlite.SQLiteException
import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.database.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.metadata.ChapterDownloadSourceDao
import br.acerola.manga.local.database.dao.metadata.ChapterRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.mapper.toDownloadSources
import br.acerola.manga.local.mapper.toModel
import br.acerola.manga.repository.di.ComicInfo
import br.acerola.manga.repository.port.ChapterManagementRepository
import br.acerola.manga.repository.port.RemoteInfoOperationsRepository
import br.acerola.manga.util.normalizeTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComicInfoChapterRepository @Inject constructor(
    private val directoryDao: MangaDirectoryDao,
    private val chapterArchiveDao: ChapterArchiveDao,
    private val mangaRemoteInfoDao: MangaRemoteInfoDao,
    private val chapterRemoteInfoDao: ChapterRemoteInfoDao,
    private val chapterDownloadSourceDao: ChapterDownloadSourceDao,
) : ChapterManagementRepository<ChapterRemoteInfoPageDto> {

    @Inject
    @ComicInfo
    lateinit var comicInfoService: RemoteInfoOperationsRepository<ChapterRemoteInfoDto, String>

    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    override suspend fun refreshMangaChapters(mangaId: Long): Either<LibrarySyncError, Unit> =
        withContext(context = Dispatchers.IO) {
            _isIndexing.value = true
            _progress.value = 0

            Either.catch {
                // NOTE: mangaId aqui é o ID do MangaDirectory
                val directory = directoryDao.getMangaDirectoryById(mangaId)
                    ?: throw Exception("Directory not found for ID: $mangaId")

                // NOTE: Busca pelo vínculo com a pasta local
                val remoteManga = mangaRemoteInfoDao.getMangaByDirectoryId(directory.id).first()
                    ?: throw Exception("Remote info not found for manga directory: ${directory.name}")

                val localChapters = chapterArchiveDao.getChaptersByMangaDirectory(directory.id).first()

                val total = localChapters.size
                if (total == 0) return@catch

                localChapters.forEachIndexed { index, archive ->
                    yield()
                    // Tenta buscar metadados no arquivo. Se falhar, apenas ignora este capítulo e segue.
                    val result = comicInfoService.searchInfo(manga = archive.path)
                        .getOrNull()
                        ?.firstOrNull()

                    if (result != null) {
                        val chapterRemoteInfoEntity = result.toModel(mangaRemoteInfoFk = remoteManga.id)
                        val chapterRemoteInfoId = chapterRemoteInfoDao.insert(chapterRemoteInfoEntity)

                        val downloadSourceEntities = result.toDownloadSources(chapterFk = chapterRemoteInfoId)
                        chapterDownloadSourceDao.insertAll(*downloadSourceEntities.toTypedArray())
                    }

                    _progress.value = ((index + 1).toFloat() / total.toFloat() * 100).toInt()
                }

                _progress.value = 100
            }.mapLeft { exception ->
                when (exception) {
                    is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                    else -> LibrarySyncError.UnexpectedError(cause = exception)
                }
            }.also {
                _isIndexing.value = false
                _progress.value = -1
            }
        }

    override fun observeChapters(mangaId: Long): StateFlow<ChapterRemoteInfoPageDto> {
        return MutableStateFlow(
            value = ChapterRemoteInfoPageDto(items = emptyList(), pageSize = 0, page = 0, total = 0)
        ).asStateFlow()
    }

    override fun observeSpecificChapters(mangaId: Long, chapters: List<String>): Flow<ChapterRemoteInfoPageDto> {
        return flowOf(
            value = ChapterRemoteInfoPageDto(items = emptyList(), pageSize = 0, page = 0, total = 0)
        )
    }

    override suspend fun getChapterPage(mangaId: Long, total: Int, page: Int, pageSize: Int): ChapterRemoteInfoPageDto {
        return ChapterRemoteInfoPageDto(items = emptyList(), pageSize, page, total)
    }
}
