package br.acerola.manga.ui.feature.main.home.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.acerola.manga.ui.common.component.ButtonType
import br.acerola.manga.ui.common.component.SmartButton
import br.acerola.manga.ui.feature.main.home.viewmodel.MangaLibraryViewModel

@Composable
fun ResetIndexManga(mangaLibraryViewModel: MangaLibraryViewModel) {
    SmartButton(type = ButtonType.ICON, modifier = Modifier.size(size = 48.dp), onClick = {
        mangaLibraryViewModel.indexLibraryFromSavedFolder()
    }) {
        Icon(
            imageVector = Icons.Default.RestartAlt, contentDescription = "Reset dos mangás"
        )
    }
}
