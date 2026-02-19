package br.acerola.manga.usecase.metadata

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import arrow.core.Either
import arrow.core.flatMap
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.author.AuthorDao
import br.acerola.manga.local.database.dao.metadata.genre.GenreDao
import br.acerola.manga.local.mapper.toDto
import br.acerola.manga.local.mapper.toModel
import br.acerola.manga.repository.di.ComicInfo
import br.acerola.manga.repository.di.Mangadex
import br.acerola.manga.repository.port.RemoteInfoOperationsRepository
import br.acerola.manga.service.archive.MangaSaveCoverService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SyncMangaMetadataUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @Mangadex private val mangadexRepository: RemoteInfoOperationsRepository<MangaRemoteInfoDto, String>,
    @ComicInfo private val comicInfoRepository: RemoteInfoOperationsRepository<MangaRemoteInfoDto, String>,
    private val mangaRemoteInfoDao: MangaRemoteInfoDao,
    private val authorDao: AuthorDao,
    private val genreDao: GenreDao,
    private val directoryDao: MangaDirectoryDao,
    private val coverService: MangaSaveCoverService
) {

    suspend fun syncFromMangadex(mangaId: Long, folderId: Long, title: String, rootUri: Uri): Either<LibrarySyncError, Unit> =
        sync(mangaId, folderId, title, rootUri, mangadexRepository).onRight {
            // NOTE: Após sync bem sucedido do MangaDex, verifica se deve gerar ComicInfo.xml
            val generateDefault = br.acerola.manga.config.preference.MetadataPreference.generateComicInfoFlow(context).first()
            if (generateDefault) {
                val directory = directoryDao.getMangaDirectoryById(folderId)
                val relations = mangaRemoteInfoDao.getMangaWithRelationsById(mangaId).first()
                if (directory != null && relations != null) {
                    comicInfoRepository.saveInfo(directory.path, relations.toDto())
                }
            }
        }

    suspend fun syncFromComicInfo(mangaId: Long, folderId: Long, title: String, folderUri: Uri, rootUri: Uri): Either<LibrarySyncError, Unit> =
        sync(mangaId, folderId, title, rootUri, comicInfoRepository, folderUri.toString())

    private suspend fun sync(
        mangaId: Long,
        folderId: Long,
        title: String,
        rootUri: Uri,
        repository: RemoteInfoOperationsRepository<MangaRemoteInfoDto, String>,
        vararg extra: String
    ): Either<LibrarySyncError, Unit> = withContext(Dispatchers.IO) {
        repository.searchInfo(manga = title, extra = extra).mapLeft {
            LibrarySyncError.NetworkError(cause = null)
        }.flatMap { fetchedList ->
            val bestMatch = fetchedList.firstOrNull()
                ?: return@flatMap Either.Left(LibrarySyncError.NetworkError(cause = Exception("No metadata found")))

            Either.catch {
                // NOTE: Se mangaId for -1, significa que é um novo registro
                val remoteId = if (mangaId != -1L) {
                    mangaRemoteInfoDao.update(bestMatch.toModel().copy(id = mangaId))
                    mangaId
                } else {
                    mangaRemoteInfoDao.insert(bestMatch.toModel())
                }

                if (remoteId != -1L) {
                    bestMatch.authors?.let {
                        authorDao.insert(it.toModel(remoteId))
                    }

                    bestMatch.genre.forEach {
                        genreDao.insert(it.toModel(remoteId))
                    }

                    bestMatch.cover?.let { dto ->
                        val directory = directoryDao.getMangaDirectoryById(folderId)
                        if (directory != null) {
                            coverService.processCover(
                                coverDto = dto,
                                rootUri = rootUri,
                                folderId = folderId,
                                mangaFolderName = directory.name,
                                mangaRemoteInfoFk = remoteId
                            )
                        }
                    }
                }
                Unit
            }.mapLeft { LibrarySyncError.DatabaseError(cause = it) }
        }
    }
}
