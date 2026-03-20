package br.acerola.manga.adapter.metadata.anilist.engine

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import arrow.core.Either
import arrow.core.flatMap
import br.acerola.manga.adapter.contract.MangaPort
import br.acerola.manga.adapter.metadata.anilist.source.AnilistFetchBannerSource
import br.acerola.manga.adapter.metadata.anilist.source.AnilistFetchCoverSource
import br.acerola.manga.adapter.metadata.anilist.source.AnilistSearchByTitleSource
import br.acerola.manga.config.preference.MangaDirectoryPreference
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.dao.metadata.relationship.AuthorDao
import br.acerola.manga.local.dao.metadata.relationship.GenreDao
import br.acerola.manga.local.dao.metadata.source.AnilistSourceDao
import br.acerola.manga.local.entity.metadata.MangaRemoteInfo
import br.acerola.manga.local.translator.toAnilistSource
import br.acerola.manga.local.translator.toModel
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.service.artwork.MangaSaveBannerService
import br.acerola.manga.service.artwork.MangaSaveCoverService
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
    private val mangaRemoteInfoDao: MangaRemoteInfoDao,
    private val anilistSourceDao: AnilistSourceDao,
    private val coverService: MangaSaveCoverService,
    private val coverFetcher: AnilistFetchCoverSource,
    private val bannerService: MangaSaveBannerService,
    private val bannerFetcher: AnilistFetchBannerSource,
    private val anilistSearchByTitleSource: AnilistSearchByTitleSource,
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
            AcerolaLogger.audit(TAG, "Initiating AniList sync for manga: $mangaId", LogSource.REPOSITORY)
            _isIndexing.value = true
            try {
                val directory = directoryDao.getMangaDirectoryById(mangaId)
                    ?: return@withContext Either.Left(
                        LibrarySyncError.UnexpectedError(cause = Exception("Directory not found: $mangaId"))
                    )

                val normalizedDirName = normalizeName(directory.name)

                anilistSearchByTitleSource.searchInfo(manga = directory.name, limit = 10)
                    .mapLeft { networkError ->
                        LibrarySyncError.UnexpectedError(cause = Exception(networkError.toString()))
                    }
                    .flatMap { results ->
                        val dto = results.find { candidate ->
                            normalizeName(candidate.title) == normalizedDirName ||
                                    normalizeName(candidate.romanji.orEmpty()) == normalizedDirName
                        } ?: results.firstOrNull()
                        ?: return@flatMap Either.Left(
                            LibrarySyncError.UnexpectedError(
                                cause = Exception("No AniList results for title: ${directory.name}")
                            )
                        )

                        Either.catch {
                            val remoteInfoId = getOrCreateRemoteInfo(mangaId, dto)
                            persistAnilistData(
                                mangaId = mangaId,
                                remoteInfoId = remoteInfoId,
                                dto = dto,
                                baseUri = baseUri
                            )
                            AcerolaLogger.audit(
                                TAG, "Successfully synced AniList metadata", LogSource.REPOSITORY,
                                mapOf("mangaId" to mangaId.toString(), "anilistId" to dto.anilistId.toString())
                            )
                        }.mapLeft { exception ->
                            LibrarySyncError.UnexpectedError(cause = exception)
                        }
                    }
            } finally {
                _isIndexing.value = false
            }
        }

    private fun normalizeName(name: String): String {
        return name.filter { it.isLetterOrDigit() }.lowercase()
    }

    override fun observeLibrary(): StateFlow<List<MangaRemoteInfoDto>> {
        return MutableStateFlow(emptyList<MangaRemoteInfoDto>()).asStateFlow()
    }

    override suspend fun refreshLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit> =
        withContext(Dispatchers.IO) {
            AcerolaLogger.audit(TAG, "Starting full library AniList refresh", LogSource.REPOSITORY)
            try {
                val directories = directoryDao.getAllMangaDirectory().firstOrNull() ?: emptyList()
                directories.forEachIndexed { index, directory ->
                    refreshManga(directory.id, baseUri)
                    _progress.value = ((index + 1) * 100 / directories.size.coerceAtLeast(1))
                }
                Either.Right(Unit)
            } catch (exception: Exception) {
                Either.Left(LibrarySyncError.UnexpectedError(cause = exception))
            }
        }

    override suspend fun rebuildLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit> =
        refreshLibrary(baseUri)

    override suspend fun incrementalScan(baseUri: Uri?): Either<LibrarySyncError, Unit> =
        withContext(Dispatchers.IO) {
            AcerolaLogger.audit(TAG, "Starting incremental AniList sync", LogSource.REPOSITORY)
            try {
                val directories = directoryDao.getAllMangaDirectory().firstOrNull() ?: emptyList()
                val toSync = directories.filter { directory ->
                    val remoteInfo =
                        mangaRemoteInfoDao.getMangaByDirectoryId(directory.id).firstOrNull() ?: return@filter true

                    val anilistSource = anilistSourceDao.getByMangaRemoteInfoFk(remoteInfo.id)
                    anilistSource == null
                }
                toSync.forEachIndexed { index, directory ->
                    refreshManga(directory.id, baseUri)
                    _progress.value = ((index + 1) * 100 / toSync.size.coerceAtLeast(1))
                }
                Either.Right(Unit)
            } catch (exception: Exception) {
                Either.Left(LibrarySyncError.UnexpectedError(cause = exception))
            }
        }

    private suspend fun getOrCreateRemoteInfo(
        directoryId: Long,
        dto: MangaRemoteInfoDto
    ): Long {
        val existing = mangaRemoteInfoDao.getMangaByDirectoryId(directoryId).firstOrNull()

        val remoteInfo = existing?.copy(
            title = dto.title ?: existing.title,
            description = dto.description ?: existing.description,
            romanji = dto.romanji ?: existing.romanji,
            status = dto.status ?: existing.status,
            publication = dto.year ?: existing.publication
        )
            ?: MangaRemoteInfo(
                title = dto.title ?: "",
                description = dto.description ?: "",
                romanji = dto.romanji ?: "",
                status = dto.status ?: "UNKNOWN",
                publication = dto.year,
                mangaDirectoryFk = directoryId
            )

        return if (existing != null) {
            mangaRemoteInfoDao.update(remoteInfo)
            existing.id
        } else {
            mangaRemoteInfoDao.insert(remoteInfo)
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

        authorDao.deleteAuthorsByMangaRemoteInfoFk(remoteInfoId)
        genreDao.deleteGenresByMangaRemoteInfoFk(remoteInfoId)

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

    companion object {

        private const val TAG = "AnilistMangaRepository"
    }
}
