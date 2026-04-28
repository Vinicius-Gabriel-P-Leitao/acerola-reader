package br.acerola.comic.module.main.config.component

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.HeroItem
import br.acerola.comic.module.main.Main
import br.acerola.comic.module.main.config.layout.ComicDirectoryAccess
import br.acerola.comic.ui.R

@Composable
fun Main.Config.Component.SelectComicDirectory(
    folderName: String?,
    onFolderSelected: (Uri?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description =
        if (folderName != null) {
            stringResource(id = R.string.description_text_selected_comic_directory, folderName)
        } else {
            stringResource(id = R.string.description_text_config_select_path_manga)
        }

    Acerola.Component.HeroItem(
        title = stringResource(id = R.string.title_text_config_select_path_manga),
        description = description,
        icon = Icons.Filled.Folder,
        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
        iconBackground = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier,
        action = {
            Main.Config.Layout.ComicDirectoryAccess(onFolderSelected = onFolderSelected)
        },
    )
}
