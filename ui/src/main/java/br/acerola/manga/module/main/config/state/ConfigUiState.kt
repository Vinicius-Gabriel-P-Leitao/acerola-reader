package br.acerola.manga.module.main.config.state

import android.net.Uri
import br.acerola.manga.config.preference.AppTheme

data class ConfigUiState(
    val selectedTheme: AppTheme = AppTheme.CATPPUCCIN,
    val folderUri: Uri? = null,
    val generateComicInfo: Boolean = true,
    val isLibraryIndexing: Boolean = false,
    val libraryProgress: Float? = null,
    val isMetadataIndexing: Boolean = false,
    val metadataProgress: Float? = null
)
