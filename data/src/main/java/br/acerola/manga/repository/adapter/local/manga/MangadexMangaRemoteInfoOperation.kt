package br.acerola.manga.repository.adapter.local.manga

import android.database.sqlite.SQLiteException
import arrow.core.Either
import arrow.core.flatMap
import br.acerola.manga.data.R
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.exception.MangadexRequestException
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.database.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.database.dao.metadata.ChapterDownloadSourceDao
import br.acerola.manga.local.database.dao.metadata.ChapterRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.database.entity.archive.ChapterArchive
import br.acerola.manga.local.database.entity.metadata.ChapterDownloadSource
import br.acerola.manga.local.database.entity.metadata.ChapterRemoteInfo
import br.acerola.manga.local.mapper.toDownloadSources
import br.acerola.manga.local.mapper.toDto
import br.acerola.manga.local.mapper.toModel
import br.acerola.manga.repository.port.ApiRepository
import br.acerola.manga.repository.port.LibraryRepository
import br.acerola.manga.repository.port.Mangadex
import br.acerola.manga.util.normalizeChapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexMangaRemoteInfoOperation @Inject constructor(
    private val chapterDao: ChapterArchiveDao,
    private val mangaRemoteInfoDao: MangaRemoteInfoDao,
    private val chapterRemoteInfoDao: ChapterRemoteInfoDao,
    private val chapterDownloadSourceDao: ChapterDownloadSourceDao,
) : LibraryRepository.MangaOperations<MangaRemoteInfoDto> {
    /**
     * Qualifier para saber que é:
     *
     * [br.acerola.manga.repository.adapter.remote.mangadex.chapter.MangadexChapterInfoService]
     */
    @Inject
    @Mangadex
    lateinit var mangadexChapterInfoService: ApiRepository.RemoteInfoOperations<ChapterRemoteInfoDto, String>

    override suspend fun rescanChaptersByManga(mangaId: Long): Either<LibrarySyncError, Unit> =
        withContext(context = Dispatchers.IO) {
            Either.catch {
                val mirrorId = mangaRemoteInfoDao.getMangaById(mangaId)
                    .mapNotNull { it?.mirrorId }
                    .firstOrNull()
                    ?: throw MangadexRequestException(
                        title = R.string.title_download_error,
                        description = R.string.description_error_download_failed
                    )
                mirrorId
            }.mapLeft { exception ->
                when (exception) {
                    is MangadexRequestException -> LibrarySyncError.NetworkError(cause = exception)
                    else -> LibrarySyncError.UnexpectedError(cause = exception)
                }
            }.flatMap { mirrorId ->
                mangadexChapterInfoService.searchInfo(manga = mirrorId, limit = 100)
                    .mapLeft { networkError ->
                        LibrarySyncError.NetworkError(cause = null)
                    }
            }.flatMap { remoteChapters ->
                Either.catch {
                    val localChapters = chapterDao.getChaptersByMangaDirectory(folderId = mangaId).first()
                    val pairs = matchRemoteWithArchive(remote = remoteChapters, local = localChapters)

                    pairs.forEach { (archive, remote) ->
                        val chapterRemoteInfoEntity = remote.toModel(mangaRemoteInfoFk = archive.folderPathFk)
                        val chapterRemoteInfoId = chapterRemoteInfoDao.insert(chapterRemoteInfoEntity)

                        val downloadSourceEntities = remote.toDownloadSources(chapterFk = chapterRemoteInfoId)
                        chapterDownloadSourceDao.insertAll(*downloadSourceEntities.toTypedArray())
                    }
                }.mapLeft { exception ->
                    when (exception) {
                        is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                        else -> LibrarySyncError.UnexpectedError(cause = exception)
                    }
                }
            }
        }

    /**
     * A inserção desses dados é feita no sync de dados.
     *
     * @return [StateFlow] contendo a lista de [MangaRemoteInfoDto] atualizada em tempo real.
     */
    override fun loadMangas(): StateFlow<List<MangaRemoteInfoDto>> {
        return mangaRemoteInfoDao.getAllMangasWithRelations().map { relationsList ->
            relationsList.map { relation ->
                relation.toDto()
            }
        }.stateIn(
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
    }

    private fun matchRemoteWithArchive(
        remote: List<ChapterRemoteInfoDto>,
        local: List<ChapterArchive>
    ): List<Pair<ChapterArchive, ChapterRemoteInfoDto>> {
        val remoteByChapter = remote
            .groupBy { it.chapter?.normalizeChapter() }
            .mapValues { (_, list) ->
                list.maxBy { it.mangadexVersion }
            }

        return local.mapNotNull { archive ->
            val key = archive.chapterSort.normalizeChapter()
            val remoteInfo = remoteByChapter[key]

            if (remoteInfo == null) {
                println("DEBUG: Falha no Match - Local: $key não encontrado no Remoto")
                return@mapNotNull null
            }

            archive to remoteInfo
        }
    }

    private suspend fun loadFirstPage(mangaId: Long): ChapterRemoteInfoPageDto {
        // TODO: Fazer isso vim de config global, procurar mais locais onde isso ocorre, talvez mudar assinatura do
        //  método pai e receber via props
        val pageSize = 20
        val total = chapterRemoteInfoDao.countChaptersByMangaRemoteInfo(mangaId)

        val initialChapter: List<ChapterRemoteInfo> = chapterRemoteInfoDao.getChaptersPaged(
            mangaId = mangaId, pageSize = pageSize, offset = 0
        ).firstOrNull() ?: emptyList()

        val initialChapterSource: List<ChapterDownloadSource> =
            initialChapter.flatMap { remoteInfo ->
                chapterDownloadSourceDao.getChapterDownloadSourceByRemoteInfoId(
                        chapterId = remoteInfo.id
                    ).firstOrNull().orEmpty()
            }

        return ChapterRemoteInfoPageDto(
            items = initialChapter.map { it.toDto(sources = initialChapterSource) },
            pageSize = pageSize,
            total = total,
            page = 0,
        )
    }
}