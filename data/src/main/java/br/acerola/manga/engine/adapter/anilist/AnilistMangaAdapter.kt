package br.acerola.manga.engine.adapter.anilist

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import arrow.core.Either
import arrow.core.flatMap
import br.acerola.manga.config.preference.MangaDirectoryPreference
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.engine.port.AnilistLinkRepository
import br.acerola.manga.engine.port.MangaPort
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.metadata.relationship.AuthorDao
import br.acerola.manga.local.database.dao.metadata.relationship.GenreDao
import br.acerola.manga.local.database.dao.metadata.source.AnilistSourceDao
import br.acerola.manga.local.mapper.toAnilistSource
import br.acerola.manga.local.mapper.toModel
import br.acerola.manga.service.assets.MangaSaveBannerService
import br.acerola.manga.service.assets.MangaSaveCoverService
import br.acerola.manga.source.adapter.anilist.AnilistFetchBannerPort
import br.acerola.manga.source.adapter.anilist.AnilistFetchCoverPort
import br.acerola.manga.source.adapter.anilist.AnilistMangaInfoPort
import br.acerola.manga.engine.di.MangadexFsOps
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnilistMangaAdapter @Inject constructor(
    private val genreDao: GenreDao,
    private val authorDao: AuthorDao,
    private val directoryDao: MangaDirectoryDao,
    private val anilistSourceDao: AnilistSourceDao,
    private val coverService: MangaSaveCoverService,
    private val bannerService: MangaSaveBannerService,
    @param:MangadexFsOps private val anilistLinkRepository: AnilistLinkRepository,
    private val anilistInfoRepository: AnilistMangaInfoPort,
    private val coverFetcher: AnilistFetchCoverPort,
    private val bannerFetcher: AnilistFetchBannerPort,
    @param:ApplicationContext private val context: Context,
) : MangaPort<MangaRemoteInfoDto> {

    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    override suspend fun refreshManga(mangaId: Long, baseUri: Uri?): Either<LibrarySyncError, Unit> =
        withContext(Dispatchers.IO) {
            _isIndexing.value = true
            try {
                anilistLinkRepository.getAnilistLink(mangaId).flatMap { link ->
                    anilistInfoRepository.searchInfo(manga = link.anilistId)
                        .mapLeft { networkError ->
                            LibrarySyncError.UnexpectedError(cause = Exception(networkError.toString()))
                        }
                        .flatMap { results ->
                            val dto = results.firstOrNull()
                                ?: return@flatMap Either.Left(
                                    LibrarySyncError.UnexpectedError(
                                        cause = Exception("No AniList results for ID: ${link.anilistId}")
                                    )
                                )

                            Either.Companion.catch {
                                persistAnilistData(
                                    mangaId = mangaId,
                                    remoteInfoId = link.remoteInfoId,
                                    dto = dto,
                                    baseUri = baseUri
                                )
                            }.mapLeft { exception ->
                                LibrarySyncError.UnexpectedError(cause = exception)
                            }
                        }
                }
            } finally {
                _isIndexing.value = false
            }
        }

    private suspend fun persistAnilistData(
        mangaId: Long,
        remoteInfoId: Long,
        dto: MangaRemoteInfoDto,
        baseUri: Uri?
    ) {
        val dtoWithId = dto.copy(id = remoteInfoId)

        anilistSourceDao.insert(dtoWithId.toAnilistSource(remoteInfoId))

        dto.authors?.let { authorDao.insert(it.toModel(mangaId = remoteInfoId)) }
        dto.genre.forEach { genreDao.insert(it.toModel(mangaId = remoteInfoId)) }

        val directory = directoryDao.getMangaDirectoryById(mangaId) ?: return

        val rootPath = baseUri?.toString()
            ?: MangaDirectoryPreference.folderUriFlow(context).firstOrNull()
            ?: return

        val rootUri = rootPath.toUri()

        dto.anilistCoverImage?.let { url ->
            coverFetcher.searchCover(url).onRight { bytes ->
                coverService.processCover(
                    rootUri = rootUri,
                    folderId = directory.id,
                    bytes = bytes,
                    coverUrl = url,
                    mangaFolderName = directory.name,
                    mangaRemoteInfoFk = remoteInfoId
                )
            }
        }

        dto.anilistBannerImage?.let { url ->
            bannerFetcher.searchCover(url).onRight { bytes ->
                bannerService.processBanner(
                    rootUri = rootUri,
                    folderId = directory.id,
                    bytes = bytes,
                    bannerUrl = url,
                    mangaFolderName = directory.name,
                    mangaRemoteInfoFk = remoteInfoId
                )
            }
        }
    }

    override fun observeLibrary(): StateFlow<List<MangaRemoteInfoDto>> {
        return MutableStateFlow(emptyList<MangaRemoteInfoDto>()).asStateFlow()
    }

    // TODO: Implementar atraves  do Page(page: $page, perPage: $perPage) {} ele faz uma busca paginada e vai ser boa
    //  para isso
    override suspend fun refreshLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit> = Either.Right(Unit)
    override suspend fun rebuildLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit> = Either.Right(Unit)
    override suspend fun incrementalScan(baseUri: Uri?): Either<LibrarySyncError, Unit> = Either.Right(Unit)

    companion object {
        private const val TAG = "AnilistMangaRepository"
    }
}