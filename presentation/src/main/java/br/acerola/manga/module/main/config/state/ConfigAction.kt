package br.acerola.manga.module.main.config.state

import android.net.Uri
import br.acerola.manga.config.preference.AppTheme
import br.acerola.manga.config.preference.FileExtension

sealed interface ConfigAction {
    data class UpdateTheme(val theme: AppTheme) : ConfigAction
    data class SelectFolder(val uri: Uri?) : ConfigAction
    data class UpdateFileExtension(val extension: FileExtension) : ConfigAction
    data class UpdateGenerateComicInfo(val enabled: Boolean) : ConfigAction
    data object DeepScanLibrary : ConfigAction
    data object QuickSyncLibrary : ConfigAction
    data object SyncMangadexMetadata : ConfigAction
}
