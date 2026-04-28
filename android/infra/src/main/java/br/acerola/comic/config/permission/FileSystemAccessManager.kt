package br.acerola.comic.config.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import arrow.core.Either
import br.acerola.comic.config.preference.ComicDirectoryPreference
import br.acerola.comic.error.message.LibrarySyncError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileSystemAccessManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        var folderUri: Uri? = null
            private set

        suspend fun saveFolderUri(uri: Uri?): Either<LibrarySyncError, Unit> {
            if (uri == null) {
                folderUri = null
                ComicDirectoryPreference.clearFolderUri(context)
                return Either.Right(Unit)
            }

            return try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                folderUri = uri
                ComicDirectoryPreference.saveFolderUri(context, uri.toString())
                Either.Right(Unit)
            } catch (securityException: SecurityException) {
                folderUri = null
                Either.Left(LibrarySyncError.FolderAccessDenied(cause = securityException))
            }
        }

        suspend fun loadFolderUri() {
            ComicDirectoryPreference
                .folderUriFlow(context)
                .firstOrNull()
                ?.let { uriString ->
                    val uri = uriString.toUri()
                    if (hasPermission(uri)) {
                        folderUri = uri
                    } else {
                        ComicDirectoryPreference.clearFolderUri(context)
                        folderUri = null
                    }
                }
        }

        fun hasPermission(uri: Uri?): Boolean {
            uri ?: return false

            val persistedUris = context.contentResolver.persistedUriPermissions
            return persistedUris.any { permission ->
                permission.uri == uri &&
                    permission.isReadPermission &&
                    permission.isWritePermission
            }
        }
    }
