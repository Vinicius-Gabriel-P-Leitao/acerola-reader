package br.acerola.manga.module.main.config

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.layout.ProgressIndicator
import br.acerola.manga.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.manga.common.viewmodel.archive.FileSystemAccessViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.common.viewmodel.metadata.MetadataSettingsViewModel
import br.acerola.manga.common.viewmodel.theme.ThemeViewModel
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.config.component.GlobalCategoryManager
import br.acerola.manga.module.main.config.component.MetadataExportSettings
import br.acerola.manga.module.main.config.component.SelectFolder
import br.acerola.manga.module.main.config.component.SyncAnilistData
import br.acerola.manga.module.main.config.component.SyncLibraryArchive
import br.acerola.manga.module.main.config.component.SyncMangadexData
import br.acerola.manga.module.main.config.component.ThemeSettings
import br.acerola.manga.module.main.config.state.ConfigAction
import br.acerola.manga.module.main.config.state.ConfigUiState
import br.acerola.manga.ui.R
import kotlinx.coroutines.launch

import br.acerola.manga.module.main.config.component.TemplateManager

@Composable
fun Main.Config.Layout.Screen(
    metadataSettingsViewModel: MetadataSettingsViewModel = hiltViewModel(),
    fileSystemAccessViewModel: FileSystemAccessViewModel = hiltViewModel(),
    mangaDirectoryViewModel: MangaDirectoryViewModel = hiltViewModel(),
    mangaDexViewModel: MangaRemoteInfoViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    templateViewModel: TemplateConfigViewModel = hiltViewModel()
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
    val allCategories by mangaDexViewModel.allCategories.collectAsState()
    val templates by templateViewModel.templates.collectAsState()

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
            ConfigAction.SyncAnilistMetadata -> mangaDexViewModel.rescanAnilistMangas()
            is ConfigAction.CreateCategory -> mangaDexViewModel.createCategory(action.name, action.color)
            is ConfigAction.DeleteCategory -> mangaDexViewModel.deleteCategory(action.id)
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
                    .verticalScroll(scrollState),
            ) {
                ConfigHeader()

                // NOTE: Arquivos Locais
                SectionHeader(stringResource(id = R.string.title_text_archive_configs_in_app))

                Main.Config.Component.SelectFolder(
                    context = context,
                    folderUri = uiState.folderUri,
                    onFolderSelected = { onAction(ConfigAction.SelectFolder(it)) }
                )

                Main.Config.Component.MetadataExportSettings(
                    enabled = uiState.generateComicInfo,
                    onCheckedChange = { onAction(ConfigAction.UpdateGenerateComicInfo(it)) }
                )

                Main.Config.Component.TemplateManager(
                    templates = templates,
                    onAddTemplate = { label, pattern -> templateViewModel.onAddTemplate(label, pattern) },
                    onDeleteTemplate = { id -> templateViewModel.onDeleteTemplate(id) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).alpha(0.3f))


                // NOTE: Aparência
                SectionHeader(stringResource(id = R.string.title_settings_appearance))

                Main.Config.Component.ThemeSettings(
                    currentTheme = uiState.selectedTheme,
                    onThemeChange = { onAction(ConfigAction.UpdateTheme(it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).alpha(0.3f))

                // NOTE: Categorias
                SectionHeader(stringResource(id = R.string.title_config_categories))

                Main.Config.Component.GlobalCategoryManager(
                    categories = allCategories,
                    onCreateCategory = { name, color -> onAction(ConfigAction.CreateCategory(name, color)) },
                    onDeleteCategory = { id -> onAction(ConfigAction.DeleteCategory(id)) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).alpha(0.3f))

                // NOTE: Biblioteca
                SectionHeader(stringResource(id = R.string.label_library_context))

                Main.Config.Component.SyncLibraryArchive(
                    onDeepScan = { onAction(ConfigAction.DeepScanLibrary) },
                    onQuickSync = { onAction(ConfigAction.QuickSyncLibrary) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).alpha(0.3f))

                // NOTE: Metadados
                SectionHeader(stringResource(id = R.string.title_sync_external_metadata))

                Main.Config.Component.SyncMangadexData(
                    onRescan = { onAction(ConfigAction.SyncMangadexMetadata) }
                )

                Main.Config.Component.SyncAnilistData(
                    onRescan = { onAction(ConfigAction.SyncAnilistMetadata) }
                )

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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
    ) {
        Text(
            text = stringResource(id = R.string.label_config_activity),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
    )
}
