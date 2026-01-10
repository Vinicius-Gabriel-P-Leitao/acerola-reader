package br.acerola.manga.repository.adapter.local.manga

import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.database.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.database.dao.metadata.ChapterDownloadSourceDao
import br.acerola.manga.local.database.dao.metadata.ChapterRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.database.entity.metadata.ChapterDownloadSource
import br.acerola.manga.local.database.entity.metadata.ChapterRemoteInfo
import br.acerola.manga.local.mapper.toDto
import br.acerola.manga.local.mapper.toPageDto
import br.acerola.manga.repository.di.Mangadex
import br.acerola.manga.repository.port.ApiRepository
import br.acerola.manga.repository.port.MangaManagementRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexMangaRepository @Inject constructor(
    private val chapterDao: ChapterArchiveDao,
    private val mangaRemoteInfoDao: MangaRemoteInfoDao,
    private val chapterRemoteInfoDao: ChapterRemoteInfoDao,
    private val chapterDownloadSourceDao: ChapterDownloadSourceDao,
) : MangaManagementRepository<MangaRemoteInfoDto> {

    /**
     * Qualifier para saber que é:
     *
     * [br.acerola.manga.repository.adapter.remote.mangadex.chapter.MangadexChapterInfoService]
     */
    @Inject
    @Mangadex
    lateinit var mangadexChapterInfoService: ApiRepository.RemoteInfoOperations<ChapterRemoteInfoDto, String>

    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    override suspend fun rescanManga(mangaId: Long): Either<LibrarySyncError, Unit> {
        TODO("Not yet implemented")
    }

    /**
     * A inserção desses dados é feita no sync de dados.
     *
     * @return [StateFlow] contendo a lista de [MangaRemoteInfoDto] atualizada em tempo real.
     */
    override fun loadMangas(): StateFlow<List<MangaRemoteInfoDto>> {
        return mangaRemoteInfoDao.getAllMangasWithRelations().map { remoteInfoRelations ->
            coroutineScope {
                remoteInfoRelations.map { remoteInfo ->
                    async(context = Dispatchers.IO) {
                        val firstPage: ChapterRemoteInfoPageDto =
                            loadFirstPage(mangaId = remoteInfo.remoteInfo.id)
                        remoteInfo.toDto(firstPage)
                    }
                }.awaitAll()
            }
        }.stateIn(
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
    }

    private suspend fun loadFirstPage(mangaId: Long): ChapterRemoteInfoPageDto {
        // TODO: Fazer isso vir de config global, procurar mais locais onde isso ocorre, talvez mudar assinatura do
        //  método pai e receber via props
        val pageSize = 20
        val total = chapterRemoteInfoDao.countChaptersByMangaRemoteInfo(mangaId)

        val initialChapter: List<ChapterRemoteInfo> = chapterRemoteInfoDao.getChaptersPaged(
            mangaId = mangaId, pageSize = pageSize, offset = 0
        )

        val initialChapterSource: List<ChapterDownloadSource> = if (initialChapter.isNotEmpty()) {
            chapterDownloadSourceDao.getChapterDownloadSourceByRemoteInfoId(
                chapterId = initialChapter.map { it.id }).first()
        } else {
            emptyList()
        }

        return initialChapter.toPageDto(
            sources = initialChapterSource, pageSize = pageSize, total = total, page = 0
        )
    }

}