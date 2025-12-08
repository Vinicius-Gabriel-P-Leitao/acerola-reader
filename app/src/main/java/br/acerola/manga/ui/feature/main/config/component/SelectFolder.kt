package br.acerola.manga.ui.feature.main.config.component

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.R
import br.acerola.manga.ui.common.component.CardType
import br.acerola.manga.ui.common.component.Divider
import br.acerola.manga.ui.common.component.SmartCard
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel
import br.acerola.manga.ui.feature.main.config.layout.FolderAccess

@Composable
fun SelectFolder(
    context: Context,
    folderAccessViewModel: FolderAccessViewModel
) {
    var selectedFolderUri by remember { mutableStateOf<String?>(value = null) }

    SmartCard(
        type = CardType.CONTENT,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 8.dp, pressedElevation = 12.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(size = 40.dp)
                        .clip(CircleShape)
                        .background(color = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(
                        contentDescription = null,
                        imageVector = Icons.Filled.Folder,
                        modifier = Modifier.size(size = 24.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }

                Spacer(modifier = Modifier.width(width = 12.dp))

                Column {
                    Text(
                        text = stringResource(id = R.string.title_text_config_select_path_manga),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = stringResource(id = R.string.description_text_config_select_path_manga),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.width(width = 12.dp))

            FolderAccess(viewModel = folderAccessViewModel) { uri ->
                selectedFolderUri = uri
            }
        }

        selectedFolderUri?.let { uriString ->
            val uri = uriString.toUri()
            val documentFile = DocumentFile.fromTreeUri(context, uri)

            Divider()

            Text(
                text = stringResource(
                    id = R.string.description_text_selected_manga_folder,
                    documentFile?.name ?: stringResource(id = R.string.message_path_not_found)
                ),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}