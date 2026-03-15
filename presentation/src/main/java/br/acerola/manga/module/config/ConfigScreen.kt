package br.acerola.manga.module.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.component.CardType
import br.acerola.manga.common.component.Divider
import br.acerola.manga.common.component.Card
import br.acerola.manga.common.layout.ProgressIndicator
import br.acerola.manga.common.viewmodel.archive.FilePreferencesViewModel
import br.acerola.manga.common.viewmodel.archive.FileSystemAccessViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.common.viewmodel.metadata.MetadataSettingsViewModel
import br.acerola.manga.common.viewmodel.theme.ThemeViewModel
import br.acerola.manga.module.config.component.MetadataExportSettings
import br.acerola.manga.module.config.component.PreferSavedFile
import br.acerola.manga.module.config.component.SelectFolder
import br.acerola.manga.module.config.component.SyncLibraryArchive
import br.acerola.manga.module.config.component.SyncMangadexData
import br.acerola.manga.module.config.component.ThemeSettings
import br.acerola.manga.presentation.R

@Composable
fun ConfigScreen(
    filePreferencesViewModel: FilePreferencesViewModel,
    fileSystemAccessViewModel: FileSystemAccessViewModel,
    mangaDirectoryViewModel: MangaDirectoryViewModel,
    mangaDexViewModel: MangaRemoteInfoViewModel,
    metadataSettingsViewModel: MetadataSettingsViewModel,
    themeViewModel: ThemeViewModel
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val libraryIndexing by mangaDirectoryViewModel.isIndexing.collectAsState()
    val libraryProgress by mangaDirectoryViewModel.progress.collectAsState()

    val metadataIndexing by mangaDexViewModel.isIndexing.collectAsState()
    val metadataProgress by mangaDexViewModel.progress.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ConfigHeader()

                PrettyConfigCard(
                    title = stringResource(id = R.string.title_settings_appearance),
                    icon = Icons.Rounded.Palette,
                    iconColor = MaterialTheme.colorScheme.primary
                ) {
                    ThemeSettings(themeViewModel)
                }

                PrettyConfigCard(
                    title = stringResource(id = R.string.title_text_archive_configs_in_app),
                    icon = Icons.Rounded.FolderOpen,
                    iconColor = MaterialTheme.colorScheme.secondary
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectFolder(context, fileSystemAccessViewModel)
                        Divider(modifier = Modifier.alpha(0.5f))

                        PreferSavedFile(filePreferencesViewModel)
                        Divider(modifier = Modifier.alpha(0.5f))

                        MetadataExportSettings(metadataSettingsViewModel)
                    }
                }

                PrettyConfigCard(
                    title = stringResource(id = R.string.label_library_context),
                    icon = Icons.Rounded.Settings,
                    iconColor = MaterialTheme.colorScheme.primary
                ) {
                    SyncLibraryArchive(mangaDirectoryViewModel)
                }

                PrettyConfigCard(
                    title = stringResource(id = R.string.title_text_mangadex_configs_in_app),
                    icon = Icons.Rounded.CloudSync,
                    iconColor = MaterialTheme.colorScheme.tertiary
                ) {
                    SyncMangadexData(mangaDexViewModel)
                }

                Spacer(modifier = Modifier.height(48.dp))
            }

            Box(
                contentAlignment = Alignment.BottomStart,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = 18.dp),
            ) {
                ProgressIndicator(
                    isLoading = libraryIndexing || metadataIndexing,
                    progress = when {
                        metadataIndexing && metadataProgress >= 0 -> metadataProgress / 100f
                        libraryIndexing && libraryProgress >= 0 -> libraryProgress / 100f
                        else -> null
                    },
                )
            }
        }
    }
}

@Composable
private fun ConfigHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(32.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(2.dp)
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = stringResource(id = R.string.label_config_activity),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun PrettyConfigCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    Card(
        type = CardType.CONTENT,
        title = null,
        modifier = Modifier.padding(horizontal = 16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(modifier = Modifier.padding(start = 12.dp)) {
                content()
            }
        }
    }
}
