package br.acerola.comic.adapter.metadata.comicinfo.engine

import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.core.net.toUri
import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ComicSyncGateway
import br.acerola.comic.adapter.contract.provider.ImageProvider
import br.acerola.comic.adapter.contract.provider.MetadataProvider
import br.acerola.comic.adapter.metadata.comicinfo.ComicInfoSourceQualifier
import br.acerola.comic.adapter.metadata.mangadex.MangadexSource
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.error.message.LibrarySyncError
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.local.dao.metadata.ComicMetadataDao
import br.acerola.comic.local.dao.metadata.relationship.AuthorDao
import br.acerola.comic.local.dao.metadata.relationship.GenreDao
import br.acerola.comic.local.dao.metadata.source.ComicInfoSourceDao
import br.acerola.comic.local.entity.metadata.ComicMetadata
import br.acerola.comic.local.translator.persistence.toComicInfoSourceEntity
import br.acerola.comic.local.translator.persistence.toEntity
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.pattern.MetadataSourcePattern
import br.acerola.comic.service.artwork.CoverSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComicInfoComicEngine
    @Inject
    constructor(
        private val genreDao: GenreDao,
        private val authorDao: AuthorDao,
        private val directoryDao: ComicDirectoryDao,
        private val coverService: CoverSaver,
        private val comicMetadataDao: ComicMetadataDao,
        private val comicInfoSourceDao: ComicInfoSourceDao,
        @param:MangadexSource private val downloadCoverService: ImageProvider<String>,
    ) : ComicSyncGateway {
        @Inject
        @ComicInfoSourceQualifier
        lateinit var comicInfoSourceService: MetadataProvider<ComicMetadataDto, String>

        private val _progress = MutableStateFlow(value = -1)
        override val progress: StateFlow<Int> = _progress.asStateFlow()

        private val _isIndexing = MutableStateFlow(value = false)
        override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

        override suspend fun refreshManga(
            mangaId: Long,
            baseUri: Uri?,
        ): Either<LibrarySyncError, Unit> =
            withContext(context = Dispatchers.IO) {
                AcerolaLogger.i(TAG, "Refreshing comic from ComicInfo.xml: $mangaId", LogSource.REPOSITORY)
                _isIndexing.value = true
                try {
                    Either
                        .catch {
                            val directory = directoryDao.getDirectoryById(mangaId) ?: return@catch

                            val fetchedListResult =
                                comicInfoSourceService.searchInfo(
                                    manga = directory.name,
                                    extra = arrayOf(directory.path),
                                )

                            val bestMatch =
                                fetchedListResult.getOrNull()?.firstOrNull() ?: run {
                                    AcerolaLogger.d(TAG, "No ComicInfo.xml found or matched for: ${directory.name}", LogSource.REPOSITORY)
                                    return@catch
                                }

                            val mangaToSave =
                                ComicMetadata(
                                    id = bestMatch.id ?: 0L,
                                    title = bestMatch.title,
                                    description = bestMatch.description,
                                    romanji = bestMatch.romanji.orEmpty(),
                                    status = bestMatch.status,
                                    publication = bestMatch.year ?: 0,
                                    mangaDirectoryFk = directory.id,
                                    syncSource = MetadataSourcePattern.COMIC_INFO.source,
                                )

                            val remoteId =
                                comicMetadataDao.upsertComicWithRelationsTransaction(
                                    metadata = mangaToSave,
                                    authors = bestMatch.authors?.let { listOf(it.toEntity(mangaId = 0L)) } ?: emptyList(),
                                    genres = bestMatch.genre.map { it.toEntity(mangaId = 0L) },
                                    comicInfoSource = bestMatch.toComicInfoSourceEntity(mangaRemoteInfoFk = 0L),
                                    authorDao = authorDao,
                                    genreDao = genreDao,
                                    comicInfoDao = comicInfoSourceDao,
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
                                            mangaRemoteInfoFk = remoteId,
                                        )
                                    }
                                }
                                AcerolaLogger.i(
                                    TAG,
                                    "Successfully updated metadata from ComicInfo for: ${directory.name}",
                                    LogSource.REPOSITORY,
                                )
                            }
                        }.mapLeft { exception ->
                            AcerolaLogger.e(
                                TAG,
                                "Error processing ComicInfo for comic: $mangaId",
                                LogSource.REPOSITORY,
                                throwable = exception,
                            )
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

        companion object {
            private const val TAG = "ComicInfoMangaRepository"
        }
    }
