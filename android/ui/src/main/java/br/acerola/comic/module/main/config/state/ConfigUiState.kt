package br.acerola.comic.module.main.config.state
import android.net.Uri
import br.acerola.comic.config.preference.AppTheme

data class ConfigUiState(
    val selectedTheme: AppTheme = AppTheme.CATPPUCCIN,
    val folderUri: Uri? = null,
    val folderName: String? = null,
    val generateComicInfo: Boolean = true,
    val metadataLanguage: String? = null,
)
