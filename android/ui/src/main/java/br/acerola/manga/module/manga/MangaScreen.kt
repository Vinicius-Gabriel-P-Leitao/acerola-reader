package br.acerola.manga.module.manga

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.GlassButton
import br.acerola.manga.common.ux.layout.TopBar
import br.acerola.manga.common.ux.component.SnackbarVariant
import br.acerola.manga.common.ux.component.showSnackbar
import br.acerola.manga.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.manga.common.viewmodel.library.archive.ChapterArchiveViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.ChapterMetadataViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaMetadataViewModel
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.config.preference.ChapterSortPreferenceData
import br.acerola.manga.module.manga.component.ChapterSortSheet
import br.acerola.manga.module.manga.layout.ChapterSection
import br.acerola.manga.module.manga.layout.Header
import br.acerola.manga.module.manga.layout.ConfigSection
import br.acerola.manga.module.manga.layout.Tabs
import br.acerola.manga.module.manga.state.MainTab
import br.acerola.manga.module.manga.state.MangaAction
import br.acerola.manga.module.manga.state.MangaChapterAction
import br.acerola.manga.module.manga.state.MangaSyncAction
import br.acerola.manga.module.manga.state.MangaUiState
import br.acerola.manga.module.reader.ReaderActivity
import br.acerola.manga.ui.R
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.launch

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MangaScreen(
    manga: MangaDto,
    onBackClick: () -> Unit,
    mangaViewModel: MangaViewModel = hiltViewModel(),
    chapterArchiveViewModel: ChapterArchiveViewModel = hiltViewModel(),
    mangaDirectoryViewModel: MangaDirectoryViewModel = hiltViewModel(),
    mangaMetadataViewModel: MangaMetadataViewModel = hiltViewModel(),
    chapterMetadataViewModel: ChapterMetadataViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(key1 = manga.directory.id) {
        mangaViewModel.init(mangaId = manga.remoteInfo?.id, folderId = manga.directory.id)
    }

    // Coleta de eventos de UI para snackbars
    LaunchedEffect(Unit) {
        launch {
            mangaViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context), SnackbarVariant.Error)
            }
        }
        launch {
            mangaDirectoryViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context), SnackbarVariant.Error)
            }
        }
        launch {
            chapterArchiveViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context), SnackbarVariant.Error)
            }
        }
        launch {
            mangaMetadataViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context), SnackbarVariant.Error)
            }
        }
        launch {
            chapterMetadataViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context), SnackbarVariant.Error)
            }
        }
    }

    var selectedTab by remember { mutableStateOf(value = MainTab.CHAPTERS) }

    val mangaState by mangaViewModel.manga.collectAsStateWithLifecycle()
    val chapterDto by mangaViewModel.chapters.collectAsStateWithLifecycle()
    val history by mangaViewModel.history.collectAsStateWithLifecycle()
    val readChapters by mangaViewModel.readChapters.collectAsStateWithLifecycle()
    val selectedChapterPerPage by mangaViewModel.selectedChapterPerPage.collectAsStateWithLifecycle()
    val chapterSortSettings by mangaViewModel.chapterSortSettings.collectAsStateWithLifecycle()
    val allCategories by mangaMetadataViewModel.allCategories.collectAsStateWithLifecycle()

    val currentManga = mangaState ?: manga
    val totalChapters = chapterDto?.archive?.total ?: 0
    val currentPage = chapterDto?.archive?.page ?: 0

    val totalPages = remember(key1 = chapterDto?.archive?.total, key2 = chapterDto?.archive?.pageSize) {
        val size = chapterDto?.archive?.pageSize ?: 1
        val total = chapterDto?.archive?.total ?: 0
        if (total == 0) 0 else kotlin.math.ceil(x = total.toDouble() / size).toInt()
    }

    val uiState = MangaUiState(
        manga = currentManga,
        chapters = chapterDto,
        selectedTab = selectedTab,
        history = history,
        readChapters = readChapters.toPersistentSet(),
        totalChapters = totalChapters,
        currentPage = currentPage,
        totalPages = totalPages,
        selectedChapterPerPage = selectedChapterPerPage,
        chapterSortSettings = chapterSortSettings,
        allCategories = allCategories.toPersistentList()
    )

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var showSortSheet by remember { mutableStateOf(false) }

    val onChapterAction: (MangaChapterAction) -> Unit = { action ->
        when (action) {
            is MangaChapterAction.ChangePage -> {
                mangaViewModel.loadPageAsync(action.page)
            }
            is MangaChapterAction.ClickChapter -> {
                val intent = Intent(context, ReaderActivity::class.java).apply {
                    putExtra(ReaderActivity.PageExtra.PAGE, action.chapter)
                    putExtra(ReaderActivity.PageExtra.MANGA_ID, uiState.manga.directory.id)
                    putExtra(ReaderActivity.PageExtra.INITIAL_PAGE, action.initialPage)
                }
                context.startActivity(intent)
            }
            is MangaChapterAction.ClickContinue -> {
                val chaptersList = uiState.chapters?.archive?.items ?: emptyList()
                val targetChapter = if (action.chapterId == -1L) {
                    chaptersList.firstOrNull()
                } else {
                    chaptersList.find { it.id == action.chapterId }
                }

                targetChapter?.let {
                    val intent = Intent(context, ReaderActivity::class.java).apply {
                        putExtra(ReaderActivity.PageExtra.PAGE, it)
                        putExtra(ReaderActivity.PageExtra.MANGA_ID, uiState.manga.directory.id)
                        putExtra(ReaderActivity.PageExtra.INITIAL_PAGE, action.lastPage)
                    }
                    context.startActivity(intent)
                }
            }
            is MangaChapterAction.ToggleReadStatus -> {
                mangaViewModel.toggleChapterReadStatus(action.chapterId)
            }
        }
    }

    val onSyncAction: (MangaSyncAction) -> Unit = { action ->
        when (action) {
            MangaSyncAction.SyncChaptersLocal -> chapterArchiveViewModel.syncChaptersByMangaDirectory(uiState.manga.directory.id)
            MangaSyncAction.RescanManga -> mangaDirectoryViewModel.rescanMangaByManga(uiState.manga.directory.id)
            MangaSyncAction.SyncMangadexInfo -> mangaMetadataViewModel.syncFromMangadex(uiState.manga.directory.id)
            MangaSyncAction.SyncMangadexChapters -> uiState.manga.remoteInfo?.id?.let { chapterMetadataViewModel.syncChaptersByMangadex(it) }
            MangaSyncAction.SyncComicInfo -> mangaMetadataViewModel.syncFromComicInfo(uiState.manga.directory.id)
            MangaSyncAction.SyncComicInfoChapters -> chapterMetadataViewModel.syncChaptersByComicInfo(uiState.manga.directory.id)
            MangaSyncAction.SyncAnilistInfo -> mangaMetadataViewModel.syncFromAnilist(uiState.manga.directory.id)
            MangaSyncAction.ExtractFirstPageAsCover -> mangaDirectoryViewModel.extractCoverFromChapter(uiState.manga.directory.id)
        }
    }

    val onAction: (MangaAction) -> Unit = { action ->
        when (action) {
            MangaAction.NavigateBack -> onBackClick()
            is MangaAction.SelectTab -> selectedTab = action.tab
            is MangaAction.UpdatePageSize -> mangaViewModel.updateChapterPerPage(action.size)
            is MangaAction.UpdateCategory -> mangaMetadataViewModel.updateMangaCategory(
                uiState.manga.directory.id,
                action.categoryId
            )
            is MangaAction.ToggleExternalSync -> mangaDirectoryViewModel.updateExternalSyncEnabled(
                uiState.manga.directory.id,
                action.enabled
            )
        }
    }

    var expandedCardId by rememberSaveable { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                item(
                    key = "header_${uiState.manga.remoteInfo?.title}", contentType = "header"
                ) {
                    Manga.Layout.Header(
                        manga = uiState.manga,
                        history = uiState.history,
                        onContinueClick = { id, page -> onChapterAction(MangaChapterAction.ClickContinue(id, page)) }
                    )
                }

                item(
                    key = "tabs_${uiState.manga.remoteInfo?.title}", contentType = "tabs"
                ) {
                    Manga.Layout.Tabs(
                        totalChapters = uiState.totalChapters,
                        activeTab = uiState.selectedTab,
                        onTabSelected = { onAction(MangaAction.SelectTab(it)) },
                    )
                }

                when (uiState.selectedTab) {
                    MainTab.CHAPTERS -> {
                        uiState.chapters?.let {
                            Manga.Layout.ChapterSection(
                                scope = this,
                                chapters = it,
                                currentPage = uiState.currentPage,
                                onPageChange = { page -> onChapterAction(MangaChapterAction.ChangePage(page)) },
                                totalPages = uiState.totalPages,
                                readChapters = uiState.readChapters.toList(),
                                onToggleRead = { id -> onChapterAction(MangaChapterAction.ToggleReadStatus(id)) },
                                onChapterClick = { chapter, _ -> onChapterAction(MangaChapterAction.ClickChapter(chapter, 0)) },
                            )
                        }
                    }

                    MainTab.SETTINGS -> {
                        Manga.Layout.ConfigSection(
                            scope = this,
                            uiState = uiState,
                            onAction = onAction,
                            onSyncAction = onSyncAction
                        )
                    }
                }

                item(
                    key = "spacer_${uiState.manga.remoteInfo?.title}", contentType = "tabs"
                ) {
                    Spacer(modifier = Modifier.height(height = 24.dp))
                }
            }
        }

        Acerola.Layout.TopBar(
            navigationIcon = {
                Acerola.Component.GlassButton(
                    onClick = { onAction(MangaAction.NavigateBack) },
                    icon = {
                        Icon(
                            tint = MaterialTheme.colorScheme.onSurface,
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.description_icon_navigation_back),
                        )
                    }
                )
            },
            actions = {
                Acerola.Component.GlassButton(
                    onClick = { showSortSheet = true },
                    icon = {
                        Icon(
                            tint = MaterialTheme.colorScheme.onSurface,
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(id = R.string.description_icon_home_filter)
                        )
                    }
                )
            }
        )

        if (showSortSheet) {
            Manga.Component.ChapterSortSheet(
                sortSettings = uiState.chapterSortSettings,
                onSortChange = { mangaViewModel.updateChapterSort(it) },
                onDismiss = { showSortSheet = false }
            )
        }
    }
}
