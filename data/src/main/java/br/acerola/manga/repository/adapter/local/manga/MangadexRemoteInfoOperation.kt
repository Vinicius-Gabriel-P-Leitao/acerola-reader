package br.acerola.manga.repository.adapter.local.manga

import br.acerola.manga.data.R
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.exception.MangadexRequestException
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexRemoteInfoOperation @Inject constructor(
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

    override suspend fun rescanChaptersByManga(mangaId: Long) = withContext(context = Dispatchers.IO) {
        val mirrorId = mangaRemoteInfoDao.getMangaById(mangaId).mapNotNull { it?.mirrorId }.firstOrNull()
        // TODO: Criar string
            ?: throw MangadexRequestException(
                title = R.string.title_download_error,
                description = R.string.description_error_download_failed
            )

        val remoteChapters = mangadexChapterInfoService.searchInfo(manga = mirrorId)
        // NOTE: Não usa o serviço por vim dados a mais do que precisamos.
        val localChapters = chapterDao.getChaptersByMangaDirectory(folderId = mangaId)

        localChapters
            .map { chapters ->
                matchRemoteWithArchive(
                    remote = remoteChapters,
                    local = chapters
                )
            }
            .collect { pairs ->
                pairs.forEach { (archive, remote) ->
                    val chapterRemoteInfoEntity = remote.toModel(mangaRemoteInfoFk = archive.folderPathFk)
                    val chapterRemoteInfoId = chapterRemoteInfoDao.insert(chapterRemoteInfoEntity)

                    val downloadSourceEntities = remote.toDownloadSources(chapterFk = chapterRemoteInfoId)
                    chapterDownloadSourceDao.insertAll(*downloadSourceEntities.toTypedArray())
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
        val remoteByChapter = remote.map { dto ->
            println("Remote $dto")
            dto.chapter?.normalizeChapter().let { it to dto }
        }.groupBy(keySelector = { it.first }, valueTransform = { it.second }).mapValues { (_, list) ->
            list.maxBy { it.mangadexVersion }
        }

        println("Remote $remoteByChapter")

        return local.mapNotNull { archive ->
            val key = archive.chapterSort.normalizeChapter()
            val remoteInfo = remoteByChapter[key] ?: return@mapNotNull null

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
                chapterDownloadSourceDao
                    .getChapterDownloadSourceByRemoteInfoId(
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