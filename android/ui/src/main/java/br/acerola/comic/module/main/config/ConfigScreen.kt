package br.acerola.comic.module.main.config

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import br.acerola.comic.common.state.LocalSnackbarHostState
import br.acerola.comic.common.ux.component.SnackbarVariant
import br.acerola.comic.common.ux.component.showSnackbar
import br.acerola.comic.common.ux.tokens.ShapeTokens
import br.acerola.comic.common.ux.tokens.SizeTokens
import br.acerola.comic.common.ux.tokens.SpacingTokens
import br.acerola.comic.common.viewmodel.archive.FileSystemAccessViewModel
import br.acerola.comic.common.viewmodel.library.archive.ComicDirectoryViewModel
import br.acerola.comic.common.viewmodel.library.metadata.ComicMetadataViewModel
import br.acerola.comic.common.viewmodel.metadata.MetadataSettingsViewModel
import br.acerola.comic.common.viewmodel.network.P2pViewModel
import br.acerola.comic.common.viewmodel.theme.ThemeViewModel
import br.acerola.comic.module.main.Main
import br.acerola.comic.module.main.config.component.GlobalCategoryManager
import br.acerola.comic.module.main.config.component.LanguageSettings
import br.acerola.comic.module.main.config.component.MetadataExportSettings
import br.acerola.comic.module.main.config.component.SelectComicDirectory
import br.acerola.comic.module.main.config.component.SyncAnilistData
import br.acerola.comic.module.main.config.component.SyncLibraryArchive
import br.acerola.comic.module.main.config.component.SyncMangadexData
import br.acerola.comic.module.main.config.component.TemplateManager
import br.acerola.comic.module.main.config.component.ThemeSettings
import br.acerola.comic.module.main.config.state.ConfigAction
import br.acerola.comic.module.main.config.state.ConfigUiState
import br.acerola.comic.ui.R
import kotlinx.coroutines.launch

@Composable
fun Main.Config.Template.Screen(
    metadataSettingsViewModel: MetadataSettingsViewModel = hiltViewModel(),
    fileSystemAccessViewModel: FileSystemAccessViewModel = hiltViewModel(),
    comicDirectoryViewModel: ComicDirectoryViewModel = hiltViewModel(),
    comicDexViewModel: ComicMetadataViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    onNavigateToTemplates: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        launch {
            fileSystemAccessViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context), SnackbarVariant.Error)
            }
        }
        launch {
            comicDirectoryViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context), SnackbarVariant.Error)
            }
        }
        launch {
            comicDexViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context), SnackbarVariant.Error)
            }
        }
        launch {
            metadataSettingsViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context), SnackbarVariant.Error)
            }
        }
        launch {
            themeViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context), SnackbarVariant.Error)
            }
        }
    }

    val selectedTheme by themeViewModel.currentTheme.collectAsState()
    val generateComicInfo by metadataSettingsViewModel.generateComicInfo.collectAsState()
    val metadataLanguage by metadataSettingsViewModel.metadataLanguage.collectAsState()
    val allCategories by comicDexViewModel.allCategories.collectAsState()
    val folderName by fileSystemAccessViewModel.folderName.collectAsState()
    val tutorialShown by fileSystemAccessViewModel.tutorialShown.collectAsState()

    val uiState =
        ConfigUiState(
            selectedTheme = selectedTheme,
            folderUri = fileSystemAccessViewModel.folderUri,
            folderName = folderName,
            generateComicInfo = generateComicInfo,
            metadataLanguage = metadataLanguage,
        )

    val onAction: (ConfigAction) -> Unit = { action ->
        when (action) {
            is ConfigAction.UpdateTheme -> themeViewModel.setTheme(action.theme)
            is ConfigAction.SelectFolder -> fileSystemAccessViewModel.saveFolderUri(action.uri)
            is ConfigAction.UpdateGenerateComicInfo -> metadataSettingsViewModel.setGenerateComicInfo(action.enabled)
            is ConfigAction.UpdateMetadataLanguage -> metadataSettingsViewModel.setMetadataLanguage(action.language)
            ConfigAction.DeepScanLibrary -> comicDirectoryViewModel.deepScanLibrary()
            ConfigAction.QuickSyncLibrary -> comicDirectoryViewModel.syncLibrary()
            ConfigAction.SyncMangadexMetadata -> comicDexViewModel.rescanMangas()
            ConfigAction.SyncAnilistMetadata -> comicDexViewModel.rescanAnilistMangas()
            is ConfigAction.CreateCategory -> comicDexViewModel.createCategory(action.name, action.color)
            is ConfigAction.DeleteCategory -> comicDexViewModel.deleteCategory(action.id)
            ConfigAction.NavigateToTemplateConfig -> onNavigateToTemplates()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .verticalScroll(scrollState),
            ) {
                if (!tutorialShown) {
                    OnboardingGuideCard()
                }

                // NOTE: Arquivos Locais
                SectionHeader(stringResource(id = R.string.title_text_archive_configs_in_app))

                Main.Config.Component.SelectComicDirectory(
                    folderName = uiState.folderName,
                    onFolderSelected = { onAction(ConfigAction.SelectFolder(it)) },
                    modifier = Modifier.padding(horizontal = SpacingTokens.Large),
                )

                Spacer(modifier = Modifier.height(SpacingTokens.Small))

                Main.Config.Component.MetadataExportSettings(
                    enabled = uiState.generateComicInfo,
                    onCheckedChange = { onAction(ConfigAction.UpdateGenerateComicInfo(it)) },
                    modifier = Modifier.padding(horizontal = SpacingTokens.Large),
                )

                Spacer(modifier = Modifier.height(SpacingTokens.Small))

                Main.Config.Component.TemplateManager(
                    onManageTemplates = { onAction(ConfigAction.NavigateToTemplateConfig) },
                    modifier = Modifier.padding(horizontal = SpacingTokens.Large),
                )

                HorizontalDivider(
                    modifier =
                        Modifier
                            .padding(horizontal = SpacingTokens.Huge, vertical = SpacingTokens.Small)
                            .alpha(0.3f),
                )

                // NOTE: Biblioteca
                SectionHeader(stringResource(id = R.string.label_library_context))

                Main.Config.Component.SyncLibraryArchive(
                    onDeepScan = { onAction(ConfigAction.DeepScanLibrary) },
                    onQuickSync = { onAction(ConfigAction.QuickSyncLibrary) },
                    modifier = Modifier.padding(horizontal = SpacingTokens.Large),
                )

                HorizontalDivider(
                    modifier =
                        Modifier
                            .padding(horizontal = SpacingTokens.Huge, vertical = SpacingTokens.Small)
                            .alpha(0.3f),
                )

                // NOTE: Aparência
                SectionHeader(stringResource(id = R.string.title_settings_appearance))

                Main.Config.Component.ThemeSettings(
                    currentTheme = uiState.selectedTheme,
                    onThemeChange = { onAction(ConfigAction.UpdateTheme(it)) },
                )

                HorizontalDivider(
                    modifier =
                        Modifier
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .alpha(0.3f),
                )

                // NOTE: Categorias
                SectionHeader(stringResource(id = R.string.title_config_categories))

                Main.Config.Component.GlobalCategoryManager(
                    categories = allCategories,
                    onCreateCategory = { name, color -> onAction(ConfigAction.CreateCategory(name, color)) },
                    onDeleteCategory = { id -> onAction(ConfigAction.DeleteCategory(id)) },
                    modifier = Modifier.padding(horizontal = SpacingTokens.Large),
                )

                HorizontalDivider(
                    modifier =
                        Modifier
                            .padding(horizontal = SpacingTokens.Huge, vertical = SpacingTokens.Small)
                            .alpha(0.3f),
                )

                // NOTE: Metadados
                SectionHeader(stringResource(id = R.string.label_sync_group))

                Main.Config.Component.LanguageSettings(
                    selectedLanguage = uiState.metadataLanguage,
                    onLanguageSelected = { onAction(ConfigAction.UpdateMetadataLanguage(it)) },
                    modifier = Modifier.padding(horizontal = SpacingTokens.Large),
                )

                Spacer(modifier = Modifier.height(SpacingTokens.Small))

                Main.Config.Component.SyncMangadexData(
                    onRescan = { onAction(ConfigAction.SyncMangadexMetadata) },
                    modifier = Modifier.padding(horizontal = SpacingTokens.Large),
                )

                Spacer(modifier = Modifier.height(SpacingTokens.Small))

                Main.Config.Component.SyncAnilistData(
                    onRescan = { onAction(ConfigAction.SyncAnilistMetadata) },
                    modifier = Modifier.padding(horizontal = SpacingTokens.Large),
                )

                HorizontalDivider(
                    modifier =
                        Modifier
                            .padding(horizontal = SpacingTokens.Huge, vertical = SpacingTokens.Small)
                            .alpha(0.3f),
                )

                // FIXME: Só descomentar quando tiver pronto a função.
                // P2pDemoSection()

                Spacer(modifier = Modifier.height(SizeTokens.ClickTarget))
            }
        }
    }
}

@Composable
private fun OnboardingGuideCard() {
    Card(
        modifier =
            Modifier
                .padding(SpacingTokens.Large)
                .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = ShapeTokens.Medium,
    ) {
        Column(modifier = Modifier.padding(SpacingTokens.Large)) {
            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(SpacingTokens.Small))
            Text(
                text = stringResource(id = R.string.title_tutorial_setup),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(SpacingTokens.ExtraSmall))
            Text(
                text = "1. " + stringResource(id = R.string.description_tutorial_folder_select),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(SpacingTokens.ExtraSmall))
            Text(
                text = "2. " + stringResource(id = R.string.description_tutorial_sync_deep),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = SpacingTokens.Huge, top = SpacingTokens.Huge, bottom = SpacingTokens.Small),
    )
}

// INFO: Demo code
@Composable
fun P2pDemoSection(p2pViewModel: P2pViewModel = hiltViewModel()) {
    val localId = remember(p2pViewModel) { p2pViewModel.getLocalId() }
    val mode = remember(p2pViewModel) { p2pViewModel.getMode() }
    val clipboardManager = LocalClipboardManager.current
    var remotePeerId by remember { mutableStateOf("") }

    SectionHeader("P2P Demo")
    Column(modifier = Modifier.padding(horizontal = SpacingTokens.Large)) {
        Row {
            Text(text = "Local ID: $localId", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { clipboardManager.setText(AnnotatedString(localId)) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
            }
        }
        Spacer(modifier = Modifier.height(SpacingTokens.Small))
        Text(text = "Mode: $mode", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(SpacingTokens.Large))

        OutlinedTextField(
            value = remotePeerId,
            onValueChange = { remotePeerId = it },
            label = { Text("Remote Peer ID") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(SpacingTokens.Small))

        Button(onClick = { p2pViewModel.connectToPeer(remotePeerId, byteArrayOf()) }) {
            Text("Connect")
        }

        Spacer(modifier = Modifier.height(SpacingTokens.Large))

        Row {
            Button(onClick = { p2pViewModel.switchToLocal() }) {
                Text("Switch to Local")
            }
            Spacer(modifier = Modifier.padding(horizontal = SpacingTokens.Small))
            Button(onClick = { p2pViewModel.switchToRelay() }) {
                Text("Switch to Relay")
            }
        }
    }
}
