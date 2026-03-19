package br.acerola.manga.module.main.config

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.Card
import br.acerola.manga.common.ux.component.Divider
import br.acerola.manga.common.ux.layout.ProgressIndicator
import br.acerola.manga.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.manga.common.viewmodel.archive.FileSystemAccessViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.common.viewmodel.metadata.MetadataSettingsViewModel
import br.acerola.manga.common.viewmodel.theme.ThemeViewModel
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.config.component.MetadataExportSettings
import br.acerola.manga.module.main.config.component.SelectFolder
import br.acerola.manga.module.main.config.component.SyncLibraryArchive
import br.acerola.manga.module.main.config.component.SyncMangadexData
import br.acerola.manga.module.main.config.component.ThemeSettings
import br.acerola.manga.module.main.config.state.ConfigAction
import br.acerola.manga.module.main.config.state.ConfigUiState
import br.acerola.manga.presentation.R
import kotlinx.coroutines.launch

@Composable
fun Main.Config.Layout.Screen(
    metadataSettingsViewModel: MetadataSettingsViewModel = hiltViewModel(),
    fileSystemAccessViewModel: FileSystemAccessViewModel = hiltViewModel(),
    mangaDirectoryViewModel: MangaDirectoryViewModel = hiltViewModel(),
    mangaDexViewModel: MangaRemoteInfoViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val snackbarHostState = LocalSnackbarHostState.current
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        launch {
            fileSystemAccessViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context))
            }
        }
        launch {
            mangaDirectoryViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context))
            }
        }
        launch {
            mangaDexViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context))
            }
        }
        launch {
            metadataSettingsViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context))
            }
        }
        launch {
            themeViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context))
            }
        }
    }

    val selectedTheme by themeViewModel.currentTheme.collectAsState()
    val generateComicInfo by metadataSettingsViewModel.generateComicInfo.collectAsState()
    
    val libraryIndexing by mangaDirectoryViewModel.isIndexing.collectAsState()
    val libraryProgress by mangaDirectoryViewModel.progress.collectAsState()

    val metadataIndexing by mangaDexViewModel.isIndexing.collectAsState()
    val metadataProgress by mangaDexViewModel.progress.collectAsState()

    val uiState = ConfigUiState(
        selectedTheme = selectedTheme,
        folderUri = fileSystemAccessViewModel.folderUri,
        generateComicInfo = generateComicInfo,
        isLibraryIndexing = libraryIndexing,
        libraryProgress = if (libraryProgress >= 0) libraryProgress / 100f else null,
        isMetadataIndexing = metadataIndexing,
        metadataProgress = if (metadataProgress >= 0) metadataProgress / 100f else null
    )

    val onAction: (ConfigAction) -> Unit = { action ->
        when (action) {
            is ConfigAction.UpdateTheme -> themeViewModel.setTheme(action.theme)
            is ConfigAction.SelectFolder -> fileSystemAccessViewModel.saveFolderUri(action.uri)
            is ConfigAction.UpdateGenerateComicInfo -> metadataSettingsViewModel.setGenerateComicInfo(action.enabled)
            ConfigAction.DeepScanLibrary -> mangaDirectoryViewModel.deepScanLibrary()
            ConfigAction.QuickSyncLibrary -> mangaDirectoryViewModel.syncLibrary()
            ConfigAction.SyncMangadexMetadata -> mangaDexViewModel.rescanMangas()
        }
    }

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
                    Main.Config.Component.ThemeSettings(
                        currentTheme = uiState.selectedTheme,
                        onThemeChange = { onAction(ConfigAction.UpdateTheme(it)) }
                    )
                }

                PrettyConfigCard(
                    title = stringResource(id = R.string.title_text_archive_configs_in_app),
                    icon = Icons.Rounded.FolderOpen,
                    iconColor = MaterialTheme.colorScheme.secondary
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Main.Config.Component.SelectFolder(
                            context = context,
                            folderUri = uiState.folderUri,
                            onFolderSelected = { onAction(ConfigAction.SelectFolder(it)) }
                        )
                        Acerola.Component.Divider(modifier = Modifier.alpha(0.5f))

                        Main.Config.Component.MetadataExportSettings(
                            enabled = uiState.generateComicInfo,
                            onCheckedChange = { onAction(ConfigAction.UpdateGenerateComicInfo(it)) }
                        )
                    }
                }

                PrettyConfigCard(
                    title = stringResource(id = R.string.label_library_context),
                    icon = Icons.Rounded.Settings,
                    iconColor = MaterialTheme.colorScheme.primary
                ) {
                    Main.Config.Component.SyncLibraryArchive(
                        onDeepScan = { onAction(ConfigAction.DeepScanLibrary) },
                        onQuickSync = { onAction(ConfigAction.QuickSyncLibrary) }
                    )
                }

                PrettyConfigCard(
                    title = stringResource(id = R.string.title_text_mangadex_configs_in_app),
                    icon = Icons.Rounded.CloudSync,
                    iconColor = MaterialTheme.colorScheme.tertiary
                ) {
                    Main.Config.Component.SyncMangadexData(
                        onRescan = { onAction(ConfigAction.SyncMangadexMetadata) }
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }

            Box(
                contentAlignment = Alignment.BottomStart,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = 18.dp),
            ) {
                Acerola.Layout.ProgressIndicator(
                    isLoading = uiState.isLibraryIndexing || uiState.isMetadataIndexing,
                    progress = when {
                        uiState.isMetadataIndexing -> uiState.metadataProgress
                        uiState.isLibraryIndexing -> uiState.libraryProgress
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
    icon: ImageVector,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Acerola.Component.Card(
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
