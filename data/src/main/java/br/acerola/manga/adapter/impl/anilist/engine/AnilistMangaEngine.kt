package br.acerola.manga.adapter.impl.anilist.engine

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import arrow.core.Either
import arrow.core.flatMap
import br.acerola.manga.config.preference.MangaDirectoryPreference
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.adapter.impl.mangadex.engine.MangadexMangaEngine
import br.acerola.manga.adapter.port.MangaPort
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.dao.metadata.relationship.AuthorDao
import br.acerola.manga.local.dao.metadata.relationship.GenreDao
import br.acerola.manga.local.dao.metadata.source.AnilistSourceDao
import br.acerola.manga.local.translator.toAnilistSource
import br.acerola.manga.local.translator.toModel
import br.acerola.manga.service.artwork.MangaSaveBannerService
import br.acerola.manga.service.artwork.MangaSaveCoverService
import br.acerola.manga.adapter.impl.anilist.source.AnilistFetchBannerSource
import br.acerola.manga.adapter.impl.anilist.source.AnilistFetchCoverSource
import br.acerola.manga.adapter.impl.anilist.source.AnilistMangaInfoSource
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
class AnilistMangaEngine @Inject constructor(
    private val genreDao: GenreDao,
    private val authorDao: AuthorDao,
    private val directoryDao: MangaDirectoryDao,
    private val anilistSourceDao: AnilistSourceDao,
    private val coverService: MangaSaveCoverService,
    private val coverFetcher: AnilistFetchCoverSource,
    private val bannerService: MangaSaveBannerService,
    private val bannerFetcher: AnilistFetchBannerSource,
    private val anilistInfoRepository: AnilistMangaInfoSource,
    private val mangadexEngine: MangadexMangaEngine,
    @param:ApplicationContext private val context: Context,
) : MangaPort<MangaRemoteInfoDto> {

    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    override suspend fun refreshManga(
        mangaId: Long,
        baseUri: Uri?
    ): Either<LibrarySyncError, Unit> =
        withContext(Dispatchers.IO) {
            _isIndexing.value = true
            try {
                mangadexEngine.getAnilistLink(mangaId).flatMap { link ->
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