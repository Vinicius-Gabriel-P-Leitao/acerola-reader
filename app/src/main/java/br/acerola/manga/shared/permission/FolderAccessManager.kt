package br.acerola.manga.shared.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import br.acerola.manga.shared.config.FolderPreferences
import kotlinx.coroutines.flow.firstOrNull

class FolderAccessManager(private val context: Context) {
    var folderUri: Uri? = null
        private set

    suspend fun saveFolderUri(uri: Uri?) {
        uri?.let {
            folderUri = it
            FolderPreferences.saveFolderUri(context, uri = it.toString())
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    suspend fun loadFolderUri() {
        FolderPreferences.folderUriFlow(context)
            .firstOrNull()?.let { uriString ->
                folderUri = uriString.toUri()
            }
    }
}