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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    private val _progress = MutableStateFlow(-1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    override suspend fun rescanChaptersByManga(mangaId: Long): Either<LibrarySyncError, Unit> =
        withContext(context = Dispatchers.IO) {
            _isIndexing.value = true
            _progress.value = 0

            Either.catch {
                // NOTE: Retorna o mirrorId -> no flatMap
                mangaRemoteInfoDao.getMangaById(mangaId).mapNotNull { it?.mirrorId }.first()
            }.mapLeft { exception ->
                when (exception) {
                    is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                    else -> LibrarySyncError.UnexpectedError(cause = exception)
                }
            }.flatMap { mirrorId ->
                mangadexChapterInfoService.searchInfo(manga = mirrorId, limit = 100, onProgress = {
                    _progress.value = it
                }).mapLeft {
                    LibrarySyncError.NetworkError(cause = null)
                }
            }.flatMap { remoteChapters ->
                _progress.value = 90
                Either.catch {
                    val localChapters = chapterDao.getChaptersByMangaDirectory(folderId = mangaId).first()
                    // TODO: Verificar se é otimização, mas as vezes API lança dado válido, porem não consigo ver
                    //  isso na UI ou DB
                    val chapterPairs = matchRemoteWithArchive(remote = remoteChapters, local = localChapters)

                    if (chapterPairs.isEmpty()) {
                        throw MangadexRequestException(
                            title = R.string.title_remote_info_null_error,
                            description = R.string.description_remote_info_null_error
                        )
                    }

                    chapterPairs.forEach { (archive, remote) ->
                        val chapterRemoteInfoEntity = remote.toModel(mangaRemoteInfoFk = archive.folderPathFk)
                        val chapterRemoteInfoId = chapterRemoteInfoDao.insert(chapterRemoteInfoEntity)

                        val downloadSourceEntities = remote.toDownloadSources(chapterFk = chapterRemoteInfoId)
                        chapterDownloadSourceDao.insertAll(*downloadSourceEntities.toTypedArray())
                    }
                    _progress.value = 100
                }.mapLeft { exception ->
                    when (exception) {
                        is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                        is MangadexRequestException -> LibrarySyncError.MangadexError(cause = exception)
                        else -> LibrarySyncError.UnexpectedError(cause = exception)
                    }
                }
            }.also {
                _isIndexing.value = false
                _progress.value = -1
            }
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
                        val firstPage: ChapterRemoteInfoPageDto = loadFirstPage(mangaId = remoteInfo.remoteInfo.id)
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

    private fun matchRemoteWithArchive(
        remote: List<ChapterRemoteInfoDto>,
        local: List<ChapterArchive>
    ): List<Pair<ChapterArchive, ChapterRemoteInfoDto>> {
        // NOTE: Organiza os capitulos e normaliza o indentifier do capitulo
        val remoteByChapter = remote.mapNotNull { dto ->
            val key = dto.chapter?.normalizeChapter()
            if (key == null) null else key to dto
        }.groupBy(keySelector = { it.first }, valueTransform = { it.second }).mapValues { (_, list) ->
            list.maxBy { it.mangadexVersion }
        }

        // NOTE: Compara com os locais usando a MESMA chave normalizada
        return local.mapNotNull { archive ->
            val chapterKey = archive.chapterSort.normalizeChapter()
            val remoteInfo = remoteByChapter[chapterKey] ?: return@mapNotNull null

            archive to remoteInfo
        }
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

        // TODO: Criar toDto
        return ChapterRemoteInfoPageDto(
            items = initialChapter.map { it.toDto(sources = initialChapterSource) },
            pageSize = pageSize,
            total = total,
            page = 0,
        )
    }
}