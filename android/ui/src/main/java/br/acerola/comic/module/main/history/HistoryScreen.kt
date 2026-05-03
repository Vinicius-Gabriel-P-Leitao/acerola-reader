package br.acerola.comic.module.main.history

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import br.acerola.comic.common.state.LocalSnackbarHostState
import br.acerola.comic.common.ux.component.SnackbarVariant
import br.acerola.comic.common.ux.component.showSnackbar
import br.acerola.comic.module.comic.ComicActivity
import br.acerola.comic.module.main.Main
import br.acerola.comic.module.main.common.component.ComicListItem
import br.acerola.comic.module.main.history.component.HistoryHeroCard
import br.acerola.comic.module.main.history.state.HistoryAction
import br.acerola.comic.module.main.history.state.HistoryUiState
import br.acerola.comic.module.reader.ReaderActivity
import br.acerola.comic.ui.R

@Composable
fun Main.History.Template.Screen(viewModel: HistoryViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val historyItems by viewModel.historyItems.collectAsState()
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { message ->
            snackbarHostState.showSnackbar(message.uiMessage.asString(context), SnackbarVariant.Error)
        }
    }

    val uiState = HistoryUiState(items = historyItems)

    val onAction: (HistoryAction) -> Unit = { action ->
        when (action) {
            is HistoryAction.ClickManga -> {
                val intent =
                    Intent(context, ComicActivity::class.java).apply {
                        putExtra(ComicActivity.ChapterExtra.COMIC, action.comic)
                    }
                context.startActivity(intent)
            }
            is HistoryAction.ClickContinue -> {
                val intent =
                    Intent(context, ReaderActivity::class.java).apply {
                        putExtra(ReaderActivity.PageExtra.MANGA_ID, action.comic.directory.id)
                        putExtra(ReaderActivity.PageExtra.CHAPTER_ID, action.history.chapterArchiveId)
                        putExtra(ReaderActivity.PageExtra.CHAPTER_SORT, action.history.chapterSort)
                        putExtra(ReaderActivity.PageExtra.INITIAL_PAGE, action.history.lastPage)
                    }
                context.startActivity(intent)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
        ) {
            if (uiState.items.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(id = R.string.description_history_empty_state),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        val firstItem = uiState.items.first()
                        Main.History.Component.HistoryHeroCard(
                            comic = firstItem.comic,
                            onClick = { onAction(HistoryAction.ClickManga(firstItem.comic)) },
                            onContinueClick = { onAction(HistoryAction.ClickContinue(firstItem.comic, firstItem.history)) },
                        )

                        if (uiState.items.size > 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    items(uiState.items.drop(1), key = { it.comic.directory.id }) { item ->
                        val chapterInfo =
                            item.history.chapterName ?: stringResource(
                                id = R.string.label_chapter_unknown,
                            )

                        val progressInfo =
                            stringResource(
                                id = R.string.label_history_chapter_progress,
                                chapterInfo,
                                item.history.lastPage + 1,
                            )

                        Main.Common.Component.ComicListItem(
                            comic = item.comic,
                            subtitle = progressInfo,
                            chapterCount = item.chapterCount,
                            isCompleted = item.history.isCompleted,
                            onPlayClick = { onAction(HistoryAction.ClickContinue(item.comic, item.history)) },
                            onClick = { onAction(HistoryAction.ClickManga(item.comic)) },
                        )
                    }
                }
            }
        }
    }
}
