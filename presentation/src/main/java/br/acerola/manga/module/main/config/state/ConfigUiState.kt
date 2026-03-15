package br.acerola.manga.module.main.config.state

import android.net.Uri
import br.acerola.manga.config.preference.FileExtension

data class ConfigUiState(
    val useDynamicColor: Boolean = false,
    val folderUri: Uri? = null,
    val selectedExtension: FileExtension = FileExtension.CBZ,
    val generateComicInfo: Boolean = true,
    val isLibraryIndexing: Boolean = false,
    val libraryProgress: Float? = null,
    val isMetadataIndexing: Boolean = false,
    val metadataProgress: Float? = null
)
