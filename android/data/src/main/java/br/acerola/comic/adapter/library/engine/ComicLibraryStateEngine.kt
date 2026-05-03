package br.acerola.comic.adapter.library.engine

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ComicLibraryWriteGateway
import br.acerola.comic.error.message.LibrarySyncError
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComicLibraryStateEngine
    @Inject
    constructor(
        private val directoryDao: ComicDirectoryDao,
        @param:ApplicationContext private val context: Context,
    ) : ComicLibraryWriteGateway {
        override suspend fun updateMangaSettings(
            comicId: Long,
            externalSyncEnabled: Boolean,
        ): Either<LibrarySyncError, Unit> =
            withContext(context = Dispatchers.IO) {
                AcerolaLogger.i(TAG, "Updating comic settings: $comicId (externalSyncEnabled=$externalSyncEnabled)", LogSource.REPOSITORY)
                try {
                    Either
                        .catch {
                            val existingManga = directoryDao.getDirectoryById(comicId) ?: return@catch
                            val updatedManga = existingManga.copy(externalSyncEnabled = externalSyncEnabled)
                            directoryDao.update(entity = updatedManga)
                        }.mapLeft { exception ->
                            AcerolaLogger.e(TAG, "Failed to update comic settings: $comicId", LogSource.REPOSITORY, throwable = exception)
                            when (exception) {
                                is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                                else -> LibrarySyncError.UnexpectedError(cause = exception)
                            }
                        }
                } finally {
                    // No progress needed for setting updates
                }
            }

        override suspend fun hideManga(comicId: Long): Either<LibrarySyncError, Unit> =
            withContext(context = Dispatchers.IO) {
                AcerolaLogger.i(TAG, "Toggling hidden state for comic: $comicId", LogSource.REPOSITORY)
                Either
                    .catch {
                        val current = directoryDao.getDirectoryById(comicId)
                        val isHidden = current?.hidden ?: false
                        directoryDao.setDirectoryHidden(comicId, hidden = !isHidden)
                    }.mapLeft { exception ->
                        AcerolaLogger.e(TAG, "Failed to toggle hidden state for comic: $comicId", LogSource.REPOSITORY, throwable = exception)
                        LibrarySyncError.UnexpectedError(cause = exception)
                    }
            }

        override suspend fun deleteManga(comicId: Long): Either<LibrarySyncError, Unit> =
            withContext(context = Dispatchers.IO) {
                AcerolaLogger.i(TAG, "Deleting comic: $comicId", LogSource.REPOSITORY)
                Either
                    .catch {
                        val directory = directoryDao.getDirectoryById(comicId) ?: return@catch
                        val folderUri = directory.path.toUri()

                        DocumentFile.fromSingleUri(context, folderUri)?.delete()
                        directoryDao.delete(entity = directory)
                    }.mapLeft { exception ->
                        AcerolaLogger.e(TAG, "Failed to delete comic: $comicId", LogSource.REPOSITORY, throwable = exception)
                        LibrarySyncError.UnexpectedError(cause = exception)
                    }
            }

        companion object {
            private const val TAG = "ComicLibraryStateEngine"
        }
    }
