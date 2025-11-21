package br.acerola.manga.shared.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import br.acerola.manga.shared.config.preference.FolderPreference
import kotlinx.coroutines.flow.firstOrNull

class FolderAccessManager(private val context: Context) {
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
                FolderPreference.saveFolderUri(context, uri.toString())
            } catch (e: SecurityException) {
                e.printStackTrace()
                folderUri = null
            }
        } else {
            folderUri = null
            FolderPreference.clearFolderUri(context)
        }
    }
    suspend fun loadFolderUri() {
        FolderPreference.folderUriFlow(context)
            .firstOrNull()?.let { uriString ->
                val uri = uriString.toUri()
                if (hasPermission(uri)) {
                    folderUri = uri
                } else {
                    FolderPreference.clearFolderUri(context)
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