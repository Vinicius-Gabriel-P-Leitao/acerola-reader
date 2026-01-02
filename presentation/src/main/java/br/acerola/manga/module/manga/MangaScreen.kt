package br.acerola.manga.module.manga

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.viewmodel.library.archive.ChapterArchiveViewModel
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.feature.R
import br.acerola.manga.module.manga.layout.MangaHeader
import br.acerola.manga.module.manga.layout.MangaTabs
import br.acerola.manga.module.manga.layout.chaptersSection
import br.acerola.manga.module.manga.layout.settingsSection

enum class MainTab(@param:StringRes val titleRes: Int) {
    CHAPTERS(titleRes = R.string.title_chapter_tabs_chapters), SETTINGS(titleRes = R.string.title_chapter_tabs_settings)
}

@Composable
fun Screen(
    chapterViewModel: ChapterArchiveViewModel, manga: MangaDto
) {
    var selectedTab by remember { mutableStateOf(value = MainTab.CHAPTERS) }

    LaunchedEffect(key1 = manga.directory.id) {
        chapterViewModel.init(directoryId = manga.directory.id, firstPage = manga.directory.chapters)
    }

    val chapterPage by chapterViewModel.chapterPage.collectAsState()
    val total = chapterPage?.total ?: 0

    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        containerColor = backgroundColor, contentColor = textColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            item {
                MangaHeader(
                    manga = manga, textColor = textColor, secondaryTextColor = secondaryTextColor
                )
            }

            item {
                MangaTabs(
                    totalChapters = total,
                    activeTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    textColor = textColor,
                    primaryColor = primaryColor,
                    secondaryTextColor = secondaryTextColor,
                )
            }

            when (selectedTab) {
                MainTab.CHAPTERS -> {
                    chaptersSection(
                        chapterPage = chapterPage,
                        textColor = textColor,
                        onChapterClick = { chapter -> /* navega */ },
                        onLoadNextPage = { chapterViewModel.loadNextPage() }
                    )

                    chapterPage?.items?.size?.let {
                        if (it < total) {
                            item {
                                LaunchedEffect(key1 = Unit) { chapterViewModel.loadNextPage() }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(all = 16.dp),
                                ) {
                                    CircularProgressIndicator(color = primaryColor)
                                }
                            }
                        }
                    }
                }

                MainTab.SETTINGS -> {
                    settingsSection()
                }
            }

            item { Spacer(modifier = Modifier.height(height = 24.dp)) }
        }
    }
}


