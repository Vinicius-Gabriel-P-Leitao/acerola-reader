package br.acerola.comic.module.main.config.component

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.acerola.comic.module.main.Main
import br.acerola.comic.module.main.config.layout.ComicDirectoryAccess
import br.acerola.comic.ui.R

@Composable
fun Main.Config.Component.SelectComicDirectory(
    folderName: String?,
    onFolderSelected: (Uri?) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = stringResource(id = R.string.title_text_config_select_path_manga),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = stringResource(id = R.string.description_text_config_select_path_manga),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (folderName != null) {
                    Text(
                        text = stringResource(
                            id = R.string.description_text_selected_comic_directory,
                            folderName
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentDescription = null
                    )
                }
            }
        },
        trailingContent = {
            Main.Config.Layout.ComicDirectoryAccess(onFolderSelected = onFolderSelected)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
