package br.acerola.manga.module.main.config.component

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.Divider
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.config.layout.FolderAccess
import br.acerola.manga.ui.R

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.graphics.Color

@Composable
fun Main.Config.Component.SelectFolder(
    context: Context,
    folderUri: Uri?,
    onFolderSelected: (Uri?) -> Unit
) {
    val documentFile = remember(folderUri) {
        folderUri?.let {
            try {
                DocumentFile.fromTreeUri(context, it)
            } catch (e: Exception) {
                null
            }
        }
    }

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
                if (folderUri != null) {
                    Text(
                        text = stringResource(
                            id = R.string.description_text_selected_manga_directory,
                            documentFile?.name ?: stringResource(id = R.string.message_path_not_found)
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
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null
                    )
                }
            }
        },
        trailingContent = {
            Main.Config.Layout.FolderAccess(onFolderSelected = onFolderSelected)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
