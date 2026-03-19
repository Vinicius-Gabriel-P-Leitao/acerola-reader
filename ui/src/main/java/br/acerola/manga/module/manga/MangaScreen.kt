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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.GlassButton
import br.acerola.manga.common.ux.layout.ProgressIndicator
import br.acerola.manga.common.ux.layout.TopBar
import br.acerola.manga.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.manga.common.viewmodel.library.archive.ChapterArchiveViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.ChapterRemoteInfoViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.error.UserMessage
import br.acerola.manga.dto.archive.ChapterFileDto
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
import kotlinx.coroutines.launch

@Composable
fun MangaScreen(
    manga: MangaDto,
    onBackClick: () -> Unit,
    mangaViewModel: MangaViewModel = hiltViewModel(),
    chapterArchiveViewModel: ChapterArchiveViewModel = hiltViewModel(),
    mangaDirectoryViewModel: MangaDirectoryViewModel = hiltViewModel(),
    mangaRemoteInfoViewModel: MangaRemoteInfoViewModel = hiltViewModel(),
    chapterRemoteInfoViewModel: ChapterRemoteInfoViewModel = hiltViewModel(),
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
                snackbarHostState.showSnackbar(message.uiMessage.asString(context))
            }
        }
        launch {
            mangaDirectoryViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context))
            }
        }
        launch {
            chapterArchiveViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context))
            }
        }
        launch {
            mangaRemoteInfoViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context))
            }
        }
        launch {
            chapterRemoteInfoViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context))
            }
        }
    }

    var selectedTab by remember { mutableStateOf(value = MainTab.CHAPTERS) }

    val mangaState by mangaViewModel.manga.collectAsState()
    val chapterDto by mangaViewModel.chapters.collectAsState()
    val chapterIsIndexing by mangaViewModel.chapterIsIndexing.collectAsState()
    val chapterProgress by mangaViewModel.chapterProgress.collectAsState()
    val mangaIsIndexing by mangaViewModel.mangaIsIndexing.collectAsState()
    val mangaProgress by mangaViewModel.mangaProgress.collectAsState()
    val mangaRemoteIndexing by mangaRemoteInfoViewModel.isIndexing.collectAsState()
    val chapterRemoteIndexing by chapterRemoteInfoViewModel.isIndexing.collectAsState()
    val history by mangaViewModel.history.collectAsState()
    val readChapters by mangaViewModel.readChapters.collectAsState()
    val selectedChapterPerPage by mangaViewModel.selectedChapterPerPage.collectAsState()

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
        isIndexing = mangaIsIndexing || chapterIsIndexing || mangaRemoteIndexing || chapterRemoteIndexing,
        indexingProgress = when {
            chapterIsIndexing && chapterProgress >= 0 -> chapterProgress / 100f
            mangaIsIndexing && mangaProgress >= 0 -> mangaProgress / 100f
            else -> null
        },
        history = history,
        readChapters = readChapters.toSet(),
        totalChapters = totalChapters,
        currentPage = currentPage,
        totalPages = totalPages,
        selectedChapterPerPage = selectedChapterPerPage
    )

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val onChapterAction: (MangaChapterAction) -> Unit = { action ->
        when (action) {
            is MangaChapterAction.ChangePage -> {
                mangaViewModel.loadPageAsync(action.page)
                coroutineScope.launch {
                    listState.animateScrollToItem(index = 2)
                }
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
            MangaSyncAction.SyncMangadexInfo -> mangaRemoteInfoViewModel.syncFromMangadex(uiState.manga.directory.id)
            MangaSyncAction.SyncMangadexChapters -> uiState.manga.remoteInfo?.id?.let { chapterRemoteInfoViewModel.syncChaptersByMangadex(it) }
            MangaSyncAction.SyncComicInfo -> mangaRemoteInfoViewModel.syncFromComicInfo(uiState.manga.directory.id)
            MangaSyncAction.SyncComicInfoChapters -> chapterRemoteInfoViewModel.syncChaptersByComicInfo(uiState.manga.directory.id)
        }
    }

    val onAction: (MangaAction) -> Unit = { action ->
        when (action) {
            MangaAction.NavigateBack -> onBackClick()
            is MangaAction.SelectTab -> selectedTab = action.tab
            is MangaAction.UpdatePageSize -> mangaViewModel.updateChapterPerPage(action.size)
        }
    }

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
            }
        )

        Box(
            contentAlignment = Alignment.BottomStart,
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 18.dp),
        ) {
            Acerola.Layout.ProgressIndicator(
                isLoading = uiState.isIndexing,
                progress = uiState.indexingProgress,
            )
        }
    }
}
