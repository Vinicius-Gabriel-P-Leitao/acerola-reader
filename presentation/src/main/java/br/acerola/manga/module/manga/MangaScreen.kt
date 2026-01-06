package br.acerola.manga.module.manga

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.metadata.chapter.ChapterFeedDto
import br.acerola.manga.feature.R
import br.acerola.manga.module.manga.layout.MangaHeader
import br.acerola.manga.module.manga.layout.MangaTabs
import br.acerola.manga.module.manga.layout.chaptersSection
import br.acerola.manga.module.manga.layout.settingsSection
import kotlinx.coroutines.launch

enum class MainTab(@param:StringRes val titleRes: Int) {
    CHAPTERS(titleRes = R.string.title_chapter_tabs_chapters), SETTINGS(titleRes = R.string.title_chapter_tabs_settings)
}

@Composable
fun MangaScreen(
    manga: MangaDto,
    mangaViewModel: MangaViewModel,
    mangaDirectoryViewModel: MangaDirectoryViewModel,
    mangaRemoteInfoViewModel: MangaRemoteInfoViewModel
) {
    LaunchedEffect(key1 = manga.directory.id) {
        mangaViewModel.init(mangaId = manga.remoteInfo?.id, folderId = manga.directory.id)
    }

    var selectedTab by remember { mutableStateOf(value = MainTab.CHAPTERS) }

    val chapterDto by mangaViewModel.chapters.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val totalChapters = chapterDto?.archive?.total ?: 0
    val currentPage = chapterDto?.archive?.page ?: 1

    val totalChaptersPerPage = remember(key1 = chapterDto?.archive?.total, key2 = chapterDto?.archive?.pageSize) {
        val size = chapterDto?.archive?.pageSize ?: 1
        val total = chapterDto?.archive?.total ?: 0

        if (total == 0) 0 else kotlin.math.ceil(x = total.toDouble() / size).toInt()
    }

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
        { chapter: ChapterFileDto, remote: ChapterFeedDto? ->
            // TODO: Navegar de forma inteligente
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            item(
                key = "header_${manga.remoteInfo?.title}", contentType = "header"
            ) {
                MangaHeader(
                    manga = manga
                )
            }

            item(
                key = "tabs_${manga.remoteInfo?.title}", contentType = "tabs"
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
                            totalPages = totalChaptersPerPage,
                            onChapterClick = handleChapterClick,
                            onPageChange = handlePageChange
                        )
                    }
                }

                MainTab.SETTINGS -> {
                    settingsSection(
                        directory = manga.directory,
                        remoteInfo = manga.remoteInfo,
                        mangaDirectoryViewModel = mangaDirectoryViewModel,
                        mangaRemoteInfoViewModel = mangaRemoteInfoViewModel
                    )
                }
            }

            item(
                key = "spacer_${manga.remoteInfo?.title}", contentType = "tabs"
            ) {
                Spacer(modifier = Modifier.height(height = 24.dp))
            }
        }
    }
}


