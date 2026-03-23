package br.acerola.manga.adapter.metadata.comicinfo.engine

import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.core.net.toUri
import arrow.core.Either
import br.acerola.manga.adapter.contract.gateway.MangaSyncGateway
import br.acerola.manga.adapter.contract.provider.ImageProvider
import br.acerola.manga.adapter.contract.provider.MetadataProvider
import br.acerola.manga.adapter.metadata.comicinfo.ComicInfoSource
import br.acerola.manga.adapter.metadata.mangadex.MangadexSource
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.dao.metadata.MangaMetadataDao
import br.acerola.manga.local.dao.metadata.relationship.AuthorDao
import br.acerola.manga.local.dao.metadata.relationship.GenreDao
import br.acerola.manga.local.dao.metadata.source.ComicInfoSourceDao
import br.acerola.manga.local.translator.persistence.toComicInfoSourceEntity
import br.acerola.manga.local.translator.persistence.toEntity
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.pattern.MetadataSource
import br.acerola.manga.service.artwork.CoverSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComicInfoMangaEngine @Inject constructor(
    private val genreDao: GenreDao,
    private val authorDao: AuthorDao,
    private val directoryDao: MangaDirectoryDao,
    private val coverService: CoverSaver,
    private val mangaMetadataDao: MangaMetadataDao,
    private val comicInfoSourceDao: ComicInfoSourceDao,
    @param:MangadexSource private val downloadCoverService: ImageProvider<String>
) : MangaSyncGateway {

    @Inject
    @ComicInfoSource
    lateinit var comicInfoSourceService: MetadataProvider<MangaMetadataDto, String>

    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    override suspend fun refreshManga(mangaId: Long, baseUri: Uri?): Either<LibrarySyncError, Unit> =
        withContext(context = Dispatchers.IO) {
            AcerolaLogger.i(TAG, "Refreshing manga from ComicInfo.xml: $mangaId", LogSource.REPOSITORY)
            _isIndexing.value = true
            try {
                Either.catch {
                    val directory = directoryDao.getMangaDirectoryById(mangaId) ?: return@catch

                    val fetchedListResult = comicInfoSourceService.searchInfo(
                        manga = directory.name,
                        extra = arrayOf(directory.path)
                    )

                    val bestMatch = fetchedListResult.getOrNull()?.firstOrNull() ?: run {
                        AcerolaLogger.d(TAG, "No ComicInfo.xml found or matched for: ${directory.name}", LogSource.REPOSITORY)
                        return@catch
                    }

                    val mangaToSave = bestMatch.toEntity().copy(
                        mangaDirectoryFk = directory.id,
                        syncSource = MetadataSource.COMIC_INFO.source
                    )

                    val remoteId = mangaMetadataDao.upsertMangaMetadataTransaction(
                        metadata = mangaToSave,
                        authors = bestMatch.authors?.let { listOf(it.toEntity(mangaId = 0L)) } ?: emptyList(),
                        genres = bestMatch.genre.map { it.toEntity(mangaId = 0L) },
                        comicInfoSource = bestMatch.toComicInfoSourceEntity(mangaRemoteInfoFk = 0L),
                        authorDao = authorDao,
                        genreDao = genreDao,
                        comicInfoDao = comicInfoSourceDao
                    )

                    if (remoteId != -1L) {
                        bestMatch.cover?.let { dto ->
                            downloadCoverService.searchMedia(dto.url).onRight { bytes ->
                                coverService.processCover(
                                    rootUri = directory.path.toUri(),
                                    folderId = directory.id,
                                    bytes = bytes,
                                    coverUrl = dto.url,
                                    mangaFolderName = directory.name,
                                    mangaRemoteInfoFk = remoteId
                                )
                            }
                        }
                        AcerolaLogger.i(TAG, "Successfully updated metadata from ComicInfo for: ${directory.name}", LogSource.REPOSITORY)
                    }
                }.mapLeft { exception ->
                    AcerolaLogger.e(TAG, "Error processing ComicInfo for manga: $mangaId", LogSource.REPOSITORY, throwable = exception)
                    when (exception) {
                        is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                        is IOException -> LibrarySyncError.DiskIOFailure(path = "Local", cause = exception)
                        else -> LibrarySyncError.UnexpectedError(cause = exception)
                    }
                }
            } finally {
                _isIndexing.value = false
            }
        }

    override suspend fun refreshLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit> = Either.Right(value = Unit)
    override suspend fun rebuildLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit> = Either.Right(value = Unit)
    override suspend fun incrementalScan(baseUri: Uri?): Either<LibrarySyncError, Unit> = Either.Right(value = Unit)

    companion object {
        private const val TAG = "ComicInfoMangaRepository"
    }
}
