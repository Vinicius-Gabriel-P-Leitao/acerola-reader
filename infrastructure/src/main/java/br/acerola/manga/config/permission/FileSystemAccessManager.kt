package br.acerola.manga.config.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import br.acerola.manga.config.preference.MangaDirectoryPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileSystemAccessManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    var folderUri: Uri? = null
        private set

    // TODO: Tratar erros de forma melhor e personalizada.
    suspend fun saveFolderUri(uri: Uri?) {
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                folderUri = uri
                MangaDirectoryPreference.saveFolderUri(context, uri.toString())
            } catch (securityException: SecurityException) {
                securityException.printStackTrace()
                folderUri = null
            }
        } else {
            folderUri = null
            MangaDirectoryPreference.clearFolderUri(context)
        }
    }

    suspend fun loadFolderUri() {
        MangaDirectoryPreference.folderUriFlow(context)
            .firstOrNull()?.let { uriString ->
                val uri = uriString.toUri()
                if (hasPermission(uri)) {
                    folderUri = uri
                } else {
                    MangaDirectoryPreference.clearFolderUri(context)
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