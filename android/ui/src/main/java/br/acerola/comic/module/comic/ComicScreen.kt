package br.acerola.comic.module.comic

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.GlassButton
import br.acerola.comic.common.ux.component.SnackbarVariant
import br.acerola.comic.common.ux.component.showSnackbar
import br.acerola.comic.common.ux.layout.TopBar
import br.acerola.comic.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.comic.common.viewmodel.library.archive.ChapterArchiveViewModel
import br.acerola.comic.common.viewmodel.library.archive.ComicDirectoryViewModel
import br.acerola.comic.common.viewmodel.library.metadata.ChapterMetadataViewModel
import br.acerola.comic.common.viewmodel.library.metadata.ComicMetadataViewModel
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.module.comic.component.ChapterSortSheet
import br.acerola.comic.module.comic.layout.Header
import br.acerola.comic.module.comic.layout.Tabs
import br.acerola.comic.module.comic.layout.chapterSection
import br.acerola.comic.module.comic.layout.configSection
import br.acerola.comic.module.comic.state.ComicAction
import br.acerola.comic.module.comic.state.ComicChapterAction
import br.acerola.comic.module.comic.state.ComicSyncAction
import br.acerola.comic.module.comic.state.ComicUiState
import br.acerola.comic.module.comic.state.MainTab
import br.acerola.comic.module.reader.ReaderActivity
import br.acerola.comic.ui.R
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.launch

@Composable
fun ComicScreen(
    comic: ComicDto,
    onBackClick: () -> Unit,
    comicViewModel: ComicViewModel = hiltViewModel(),
    comicMetadataViewModel: ComicMetadataViewModel = hiltViewModel(),
    chapterArchiveViewModel: ChapterArchiveViewModel = hiltViewModel(),
    comicDirectoryViewModel: ComicDirectoryViewModel = hiltViewModel(),
    chapterMetadataViewModel: ChapterMetadataViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(key1 = comic.directory.id) {
        comicViewModel.init(comicId = comic.remoteInfo?.id, folderId = comic.directory.id)
    }

    // Coleta de eventos de UI para snackbars
    LaunchedEffect(Unit) {
        launch {
            comicViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context), SnackbarVariant.Error)
            }
        }
        launch {
            comicDirectoryViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context), SnackbarVariant.Error)
            }
        }
        launch {
            chapterArchiveViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context), SnackbarVariant.Error)
            }
        }
        launch {
            comicMetadataViewModel.uiEvents.collect { message ->
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

    val comicState by comicViewModel.comic.collectAsStateWithLifecycle()
    val chapterDto by comicViewModel.chapters.collectAsStateWithLifecycle()
    val history by comicViewModel.history.collectAsStateWithLifecycle()
    val readChapters by comicViewModel.readChapters.collectAsStateWithLifecycle()
    val selectedChapterPerPage by comicViewModel.selectedChapterPerPage.collectAsStateWithLifecycle()
    val chapterSortSettings by comicViewModel.chapterSortSettings.collectAsStateWithLifecycle()
    val allCategories by comicMetadataViewModel.allCategories.collectAsStateWithLifecycle()
    val volumeViewMode by comicViewModel.volumeViewMode.collectAsStateWithLifecycle()
    val activeVolumeId by comicViewModel.activeVolumeId.collectAsStateWithLifecycle()

    val currentManga = comicState ?: comic
    val totalChapters = chapterDto?.archive?.total ?: 0
    val currentPage = chapterDto?.archive?.page ?: 0

    val totalPages =
        remember(key1 = chapterDto?.archive?.total, key2 = chapterDto?.archive?.pageSize) {
            val size = chapterDto?.archive?.pageSize ?: 1
            val total = chapterDto?.archive?.total ?: 0
            if (total == 0) 0 else kotlin.math.ceil(x = total.toDouble() / size).toInt()
        }

    val showVolumeHeaders =
        remember(key1 = chapterDto?.showVolumeHeaders) {
            chapterDto?.showVolumeHeaders ?: false
        }

    val uiState =
        ComicUiState(
            comic = currentManga,
            chapters = chapterDto,
            selectedTab = selectedTab,
            history = history,
            readChapters = readChapters.toPersistentSet(),
            totalChapters = totalChapters,
            currentPage = currentPage,
            totalPages = totalPages,
            selectedChapterPerPage = selectedChapterPerPage,
            chapterSortSettings = chapterSortSettings,
            allCategories = allCategories.toPersistentList(),
            showVolumeHeaders = showVolumeHeaders,
            volumeViewMode = volumeViewMode,
            activeVolumeId = activeVolumeId,
            hasVolumeStructure = chapterDto?.hasVolumeStructure ?: false,
        )

    val listState = rememberLazyListState()

    var showSortSheet by remember { mutableStateOf(false) }

    val onChapterAction: (ComicChapterAction) -> Unit = { action ->
        when (action) {
            is ComicChapterAction.ChangePage -> {
                comicViewModel.loadPageAsync(action.page)
            }
            is ComicChapterAction.ClickChapter -> {
                val intent =
                    Intent(context, ReaderActivity::class.java).apply {
                        putExtra(ReaderActivity.PageExtra.PAGE, action.chapter)
                        putExtra(ReaderActivity.PageExtra.MANGA_ID, uiState.comic.directory.id)
                        putExtra(ReaderActivity.PageExtra.INITIAL_PAGE, action.initialPage)
                    }
                context.startActivity(intent)
            }
            is ComicChapterAction.ClickContinue -> {
                val chaptersList = uiState.chapters?.archive?.items ?: emptyList()
                val targetChapter =
                    if (action.chapterId == -1L) {
                        chaptersList.firstOrNull()
                    } else {
                        chaptersList.find { it.id == action.chapterId }
                    }

                targetChapter?.let {
                    val intent =
                        Intent(context, ReaderActivity::class.java).apply {
                            putExtra(ReaderActivity.PageExtra.PAGE, it)
                            putExtra(ReaderActivity.PageExtra.MANGA_ID, uiState.comic.directory.id)
                            putExtra(ReaderActivity.PageExtra.INITIAL_PAGE, action.lastPage)
                        }
                    context.startActivity(intent)
                }
            }
            is ComicChapterAction.ToggleReadStatus -> {
                comicViewModel.toggleChapterReadStatus(action.chapterSort)
            }
        }
    }

    val onSyncAction: (ComicSyncAction) -> Unit = { action ->
        when (action) {
            ComicSyncAction.SyncChaptersLocal -> chapterArchiveViewModel.syncChaptersByMangaDirectory(uiState.comic.directory.id)
            ComicSyncAction.RescanComic -> comicDirectoryViewModel.rescanMangaByManga(uiState.comic.directory.id)
            ComicSyncAction.SyncMangadexInfo -> comicMetadataViewModel.syncFromMangadex(uiState.comic.directory.id)
            ComicSyncAction.SyncMangadexChapters ->
                uiState.comic.remoteInfo?.id?.let {
                    chapterMetadataViewModel.syncChaptersByMangadex(
                        it,
                    )
                }
            ComicSyncAction.SyncComicInfo -> comicMetadataViewModel.syncFromComicInfo(uiState.comic.directory.id)
            ComicSyncAction.SyncComicInfoChapters -> chapterMetadataViewModel.syncChaptersByComicInfo(uiState.comic.directory.id)
            ComicSyncAction.SyncAnilistInfo -> comicMetadataViewModel.syncFromAnilist(uiState.comic.directory.id)
            ComicSyncAction.ExtractFirstPageAsCover -> comicDirectoryViewModel.extractCoverFromChapter(uiState.comic.directory.id)
        }
    }

    val onAction: (ComicAction) -> Unit = { action ->
        when (action) {
            ComicAction.NavigateBack -> onBackClick()
            is ComicAction.SelectTab -> selectedTab = action.tab
            is ComicAction.UpdatePageSize -> comicViewModel.updateChapterPerPage(action.size)
            is ComicAction.UpdateCategory ->
                comicMetadataViewModel.updateMangaCategory(
                    uiState.comic.directory.id,
                    action.categoryId,
                )
            is ComicAction.ToggleExternalSync ->
                comicDirectoryViewModel.updateExternalSyncEnabled(
                    uiState.comic.directory.id,
                    action.enabled,
                )
            is ComicAction.UpdateVolumeView -> comicViewModel.updateVolumeViewMode(action.mode)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(bottom = paddingValues.calculateBottomPadding()),
            ) {
                item(
                    key = "header_${uiState.comic.remoteInfo?.title}",
                    contentType = "header",
                ) {
                    Comic.Layout.Header(
                        comic = uiState.comic,
                        history = uiState.history,
                        onContinueClick = { id, page -> onChapterAction(ComicChapterAction.ClickContinue(id, page)) },
                    )
                }

                item(
                    key = "tabs_${uiState.comic.remoteInfo?.title}",
                    contentType = "tabs",
                ) {
                    Comic.Layout.Tabs(
                        totalChapters = uiState.totalChapters,
                        activeTab = uiState.selectedTab,
                        onTabSelected = { onAction(ComicAction.SelectTab(it)) },
                    )
                }

                when (uiState.selectedTab) {
                    MainTab.CHAPTERS -> {
                        uiState.chapters?.let {
                            Comic.Layout.chapterSection(
                                scope = this,
                                chapters = it,
                                currentPage = uiState.currentPage,
                                onPageChange = { page -> onChapterAction(ComicChapterAction.ChangePage(page)) },
                                totalPages = uiState.totalPages,
                                readChapters = uiState.readChapters.toList(),
                                onToggleRead = { id -> onChapterAction(ComicChapterAction.ToggleReadStatus(id)) },
                                onChapterClick = { chapter, _ -> onChapterAction(ComicChapterAction.ClickChapter(chapter, 0)) },
                                volumeViewMode = uiState.volumeViewMode,
                                activeVolumeId = uiState.activeVolumeId,
                                onSetActiveVolume = comicViewModel::setActiveVolume,
                                onUpdateVolumeView = { mode -> onAction(ComicAction.UpdateVolumeView(mode)) },
                                onLoadVolumeChaptersPage = comicViewModel::loadVolumeChaptersPage,
                            )
                        }
                    }

                    MainTab.SETTINGS -> {
                        Comic.Layout.configSection(
                            scope = this,
                            uiState = uiState,
                            onAction = onAction,
                            onSyncAction = onSyncAction,
                        )
                    }
                }

                item(
                    key = "spacer_${uiState.comic.remoteInfo?.title}",
                    contentType = "tabs",
                ) {
                    Spacer(modifier = Modifier.height(height = 24.dp))
                }
            }
        }

        Acerola.Layout.TopBar(
            navigationIcon = {
                Acerola.Component.GlassButton(
                    onClick = { onAction(ComicAction.NavigateBack) },
                    icon = {
                        Icon(
                            tint = MaterialTheme.colorScheme.onSurface,
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.description_icon_navigation_back),
                        )
                    },
                )
            },
            actions = {
                Acerola.Component.GlassButton(
                    onClick = { showSortSheet = true },
                    icon = {
                        Icon(
                            tint = MaterialTheme.colorScheme.onSurface,
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(id = R.string.description_icon_home_filter),
                        )
                    },
                )
            },
        )

        if (showSortSheet) {
            Comic.Component.ChapterSortSheet(
                sortSettings = uiState.chapterSortSettings,
                onSortChange = { comicViewModel.updateChapterSort(it) },
                onDismiss = { showSortSheet = false },
            )
        }
    }
}
