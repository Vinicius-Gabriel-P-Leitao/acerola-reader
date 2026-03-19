package br.acerola.manga.repository.adapter.local.manga

import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.core.net.toUri
import arrow.core.Either
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.relationship.AuthorDao
import br.acerola.manga.local.database.dao.metadata.relationship.GenreDao
import br.acerola.manga.local.mapper.toModel
import br.acerola.manga.repository.di.ComicInfo
import br.acerola.manga.repository.port.MangaManagementRepository
import br.acerola.manga.repository.port.RemoteInfoOperationsRepository
import br.acerola.manga.service.archive.MangaSaveCoverService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComicInfoMangaRepository @Inject constructor(
    private val genreDao: GenreDao,
    private val authorDao: AuthorDao,
    private val directoryDao: MangaDirectoryDao,
    private val coverService: MangaSaveCoverService,
    private val mangaRemoteInfoDao: MangaRemoteInfoDao,
) : MangaManagementRepository<MangaRemoteInfoDto> {

    @Inject
    @ComicInfo
    lateinit var comicInfoService: RemoteInfoOperationsRepository<MangaRemoteInfoDto, String>

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

                    val fetchedListResult = comicInfoService.searchInfo(
                        manga = directory.name,
                        extra = arrayOf(directory.path)
                    )

                    val bestMatch = fetchedListResult.getOrNull()?.firstOrNull() ?: run {
                        AcerolaLogger.d(TAG, "No ComicInfo.xml found or matched for: ${directory.name}", LogSource.REPOSITORY)  
                        return@catch
                    }

                    val existingRemote = mangaRemoteInfoDao.getMangaByDirectoryId(directory.id).firstOrNull()

                    val mangaToSave = bestMatch.toModel().copy(
                        id = existingRemote?.id ?: 0L,
                        mangaDirectoryFk = directory.id,
                        mirrorId = bestMatch.mirrorId 
                    )

                    val remoteId = if (existingRemote != null) {
                        mangaRemoteInfoDao.update(entity = mangaToSave)
                        existingRemote.id
                    } else {
                        mangaRemoteInfoDao.insert(entity = mangaToSave)
                    }

                    if (remoteId != -1L) {
                        bestMatch.authors?.let {
                            authorDao.insert(entity = it.toModel(mangaId = remoteId))
                        }

                        bestMatch.genre.forEach {
                            genreDao.insert(entity = it.toModel(mangaId = remoteId))
                        }

                        bestMatch.cover?.let { dto ->
                            coverService.processCover(
                                coverDto = dto,
                                rootUri = directory.path.toUri(), 
                                folderId = directory.id,
                                mangaFolderName = directory.name,
                                mangaRemoteInfoFk = remoteId
                            )
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

    override fun observeLibrary(): StateFlow<List<MangaRemoteInfoDto>> {
        return MutableStateFlow(value = emptyList<MangaRemoteInfoDto>()).asStateFlow()
    }

    override suspend fun refreshLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit> = Either.Right(value = Unit)
    override suspend fun rebuildLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit> = Either.Right(value = Unit)
    override suspend fun incrementalScan(baseUri: Uri?): Either<LibrarySyncError, Unit> = Either.Right(value = Unit)

    companion object {
        private const val TAG = "ComicInfoMangaRepository"  
    }
}
