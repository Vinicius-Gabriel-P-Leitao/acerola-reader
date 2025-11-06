package br.acerola.manga.ui.feature.main.config.screen

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import br.acerola.manga.R
import br.acerola.manga.shared.permission.FolderAccessManager
import br.acerola.manga.ui.common.component.CardType
import br.acerola.manga.ui.common.component.Divider
import br.acerola.manga.ui.common.component.SmartCard
import br.acerola.manga.ui.common.theme.AcerolaTheme
import br.acerola.manga.ui.common.viewmodel.archive.file.FilePreferencesViewModel
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModelFactory
import br.acerola.manga.ui.feature.main.config.component.FilePreferenceScreen
import br.acerola.manga.ui.feature.main.config.component.FolderAccessScreen

@Composable
fun ConfigScreen() {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val application = activity.application

    val folderAccessViewModel: FolderAccessViewModel = viewModel(
        factory = FolderAccessViewModelFactory(
            application = application,
            manager = FolderAccessManager(context)
        )
    )

    val filePreferencesViewModel: FilePreferencesViewModel = viewModel()

    AcerolaTheme {
        Scaffold(modifier = Modifier.padding(all = 6.dp)) { _ ->
            Column {
                SmartCard(
                    type = CardType.CONTENT,
                    title = context.getString(R.string.description_title_text_archive_configs_in_app),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                ) {
                    SelectFolderCard(
                        context = context,
                        folderAccessViewModel = folderAccessViewModel
                    )
                    Spacer(modifier = Modifier.height(height = 12.dp))
                    SelectedPreferSaveFile(
                        context = context,
                        filePreferencesViewModel = filePreferencesViewModel
                    )
                }

                Spacer(modifier = Modifier.height(height = 12.dp))

                SmartCard(
                    type = CardType.CONTENT,
                    title = context.getString(R.string.description_title_text_mangadex_configs_in_app),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                ) {
                    // TODO: Conteúdo das configs do MangaDex aqui
                }
            }
        }
    }
}

@Composable
private fun SelectFolderCard(
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
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
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
                        text = context.getString(R.string.description_title_text_config_select_path_manga),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = context.getString(R.string.description_text_config_select_path_manga),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.width(width = 12.dp))

            FolderAccessScreen(
                context = context,
                viewModel = folderAccessViewModel
            ) { uri ->
                selectedFolderUri = uri
            }
        }

        selectedFolderUri?.let { uriString ->
            val uri = uriString.toUri()
            val documentFile = DocumentFile.fromTreeUri(context, uri)

            Divider()

            Text(
                text = context.getString(
                    R.string.description_text_selected_manga_folder,
                    documentFile?.name ?: context.getString(R.string.message_path_not_found)
                ),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SelectedPreferSaveFile(
    context: Context,
    filePreferencesViewModel: FilePreferencesViewModel
) {
    SmartCard(
        type = CardType.CONTENT,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(size = 40.dp)
                        .clip(CircleShape)
                        .background(color = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FileOpen,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(size = 22.dp),
                        contentDescription = context.getString(
                            R.string.description_icon_select_preference_saved_file
                        ),
                    )
                }

                Spacer(modifier = Modifier.width(width = 12.dp))

                Column {
                    Text(
                        text = context.getString(R.string.description_title_preference_file_extension),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        text = context.getString(
                            R.string.description_text_preference_file_extension_default
                        ),
                    )
                }
            }

            Divider()

            FilePreferenceScreen(filePreferencesViewModel)
        }
    }
}