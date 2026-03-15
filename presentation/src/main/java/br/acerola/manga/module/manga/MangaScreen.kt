package br.acerola.manga.module.manga

import android.content.Intent
import androidx.annotation.StringRes
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
import br.acerola.manga.common.component.Acerola
import br.acerola.manga.common.component.TopBar
import br.acerola.manga.common.component.GlassButton
import br.acerola.manga.common.layout.ProgressIndicator
import br.acerola.manga.common.viewmodel.library.archive.ChapterArchiveViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.ChapterRemoteInfoViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.metadata.chapter.ChapterFeedDto
import br.acerola.manga.module.manga.layout.MangaHeader
import br.acerola.manga.module.manga.layout.MangaTabs
import br.acerola.manga.module.manga.layout.chaptersSection
import br.acerola.manga.module.manga.layout.configSection
import br.acerola.manga.module.reader.ReaderActivity
import br.acerola.manga.presentation.R
import kotlinx.coroutines.launch

enum class MainTab(@param:StringRes val titleRes: Int) {
    CHAPTERS(titleRes = R.string.title_chapter_tabs_chapters),
    SETTINGS(titleRes = R.string.title_chapter_tabs_settings)
}

@Composable
fun MangaScreen(
    manga: MangaDto,
    mangaViewModel: MangaViewModel,
    chapterArchiveViewModel: ChapterArchiveViewModel,
    mangaDirectoryViewModel: MangaDirectoryViewModel,
    mangaRemoteInfoViewModel: MangaRemoteInfoViewModel,
    chapterRemoteInfoViewModel: ChapterRemoteInfoViewModel,
    onBackClick: () -> Unit,
) {
    LaunchedEffect(key1 = manga.directory.id) {
        mangaViewModel.init(mangaId = manga.remoteInfo?.id, folderId = manga.directory.id)
    }

    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(value = MainTab.CHAPTERS) }

    // NOTE: Vai sobrescrever o que vem do Intent, isso é necessário para atualizar UI
    val mangaState by mangaViewModel.manga.collectAsState()
    val currentManga = mangaState ?: manga

    // NOTE: Atualizar se a função de sync de métadados for chamada
    val chapterDto by mangaViewModel.chapters.collectAsState()

    val chapterIsIndexing by mangaViewModel.chapterIsIndexing.collectAsState()
    val chapterProgress by mangaViewModel.chapterProgress.collectAsState()

    val mangaIsIndexing by mangaViewModel.mangaIsIndexing.collectAsState()
    val mangaProgress by mangaViewModel.mangaProgress.collectAsState()

    val mangaRemoteIndexing by mangaRemoteInfoViewModel.isIndexing.collectAsState()
    val chapterRemoteIndexing by chapterRemoteInfoViewModel.isIndexing.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val totalChapters = chapterDto?.archive?.total ?: 0
    val currentPage = chapterDto?.archive?.page ?: 1

    val totalChaptersPerPage = remember(key1 = chapterDto?.archive?.total, key2 = chapterDto?.archive?.pageSize) {
        val size = chapterDto?.archive?.pageSize ?: 1
        val total = chapterDto?.archive?.total ?: 0

        if (total == 0) 0 else kotlin.math.ceil(x = total.toDouble() / size).toInt()
    }

    val history by mangaViewModel.history.collectAsState()
    val readChapters by mangaViewModel.readChapters.collectAsState()

    val handlePageChange = remember(key1 = mangaViewModel, key2 = listState) {
        { nextPage: Int ->
            mangaViewModel.loadPageAsync(nextPage)

            coroutineScope.launch {
                listState.animateScrollToItem(index = 2)
            }

            Unit
        }
    }

    val handleChapterClick = remember {
        { chapter: ChapterFileDto, remote: ChapterFeedDto?, initialPage: Int ->
            val intent = Intent(context, ReaderActivity::class.java).apply {
                putExtra(ReaderActivity.PageExtra.PAGE, chapter)
                putExtra(ReaderActivity.PageExtra.MANGA_ID, currentManga.directory.id)
                putExtra(ReaderActivity.PageExtra.INITIAL_PAGE, initialPage)
            }

            context.startActivity(intent)
        }
    }

    val handleContinueClick = { chapterId: Long, lastPage: Int ->
        val chaptersList = chapterDto?.archive?.items ?: emptyList()
        val targetChapter = if (chapterId == -1L) {
            chaptersList.firstOrNull()
        } else {
            chaptersList.find { it.id == chapterId }
        }

        targetChapter?.let {
            handleChapterClick(it, null, lastPage)
        }
        Unit
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
                    key = "header_${currentManga.remoteInfo?.title}", contentType = "header"
                ) {
                    MangaHeader(
                        manga = currentManga,
                        history = history,
                        onContinueClick = handleContinueClick
                    )
                }

                item(
                    key = "tabs_${currentManga.remoteInfo?.title}", contentType = "tabs"
                ) {
                    MangaTabs(
                        totalChapters = totalChapters,
                        activeTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                    )
                }

                when (selectedTab) {
                    MainTab.CHAPTERS -> {
                        chapterDto?.let {
                            chaptersSection(
                                chapters = it,
                                currentPage = currentPage,
                                onPageChange = handlePageChange,
                                totalPages = totalChaptersPerPage,
                                readChapters = readChapters,
                                onToggleRead = { id -> mangaViewModel.toggleChapterReadStatus(id) },
                                onChapterClick = { chapter, remote -> handleChapterClick(chapter, remote, 0) },
                            )
                        }
                    }

                    MainTab.SETTINGS -> {
                        configSection(
                            mangaViewModel = mangaViewModel,
                            directory = currentManga.directory,
                            remoteInfo = currentManga.remoteInfo,
                            mangaDirectoryViewModel = mangaDirectoryViewModel,
                            chapterArchiveViewModel = chapterArchiveViewModel,
                            mangaRemoteInfoViewModel = mangaRemoteInfoViewModel,
                            chapterRemoteInfoViewModel = chapterRemoteInfoViewModel
                        )
                    }
                }

                item(
                    key = "spacer_${currentManga.remoteInfo?.title}", contentType = "tabs"
                ) {
                    Spacer(modifier = Modifier.height(height = 24.dp))
                }
            }
        }

        // Floating Top Bar with Glass Back Button
        Acerola.TopBar(
            navigationIcon = {
                Acerola.GlassButton(
                    onClick = onBackClick,
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.description_icon_navigation_back),
                            tint = MaterialTheme.colorScheme.onSurface
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
            ProgressIndicator(
                isLoading = mangaIsIndexing || chapterIsIndexing || mangaRemoteIndexing || chapterRemoteIndexing,
                progress = when {
                    chapterIsIndexing && chapterProgress >= 0 -> chapterProgress / 100f
                    mangaIsIndexing && mangaProgress >= 0 -> mangaProgress / 100f
                    else -> null
                },
            )
        }
    }
}