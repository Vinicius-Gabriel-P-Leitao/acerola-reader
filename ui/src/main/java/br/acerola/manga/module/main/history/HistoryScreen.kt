package br.acerola.manga.module.main.history

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import br.acerola.manga.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.common.component.MangaListItem
import br.acerola.manga.module.main.history.state.HistoryAction
import br.acerola.manga.module.main.history.state.HistoryUiState
import br.acerola.manga.module.manga.MangaActivity
import br.acerola.manga.module.reader.ReaderActivity
import br.acerola.manga.ui.R

@Composable
fun Main.History.Layout.Screen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val historyItems by viewModel.historyItems.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { message ->
            snackbarHostState.showSnackbar(message.uiMessage.asString(context))
        }
    }

    val uiState = HistoryUiState(items = historyItems)

    val onAction: (HistoryAction) -> Unit = { action ->
        when (action) {
            is HistoryAction.ClickManga -> {
                val intent = Intent(context, MangaActivity::class.java).apply {
                    putExtra(MangaActivity.ChapterExtra.MANGA, action.manga)
                }
                context.startActivity(intent)
            }
            is HistoryAction.ClickContinue -> {
                val intent = Intent(context, ReaderActivity::class.java).apply {
                    putExtra(ReaderActivity.PageExtra.MANGA_ID, action.manga.directory.id)
                    putExtra(ReaderActivity.PageExtra.CHAPTER_ID, action.history.chapterArchiveId)
                    putExtra(ReaderActivity.PageExtra.INITIAL_PAGE, action.history.lastPage)
                }
                context.startActivity(intent)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.title_history_screen),
                modifier = Modifier.padding(vertical = 16.dp),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
            )

            if (uiState.items.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.description_history_empty_state),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.items, key = { it.manga.directory.id }) { item ->
                        val chapterInfo =
                            item.history.chapterName ?: stringResource(id = R.string.label_chapter_unknown)
                        val progressInfo = stringResource(
                            id = R.string.label_history_chapter_progress,
                            chapterInfo,
                            item.history.lastPage + 1
                        )

                        Main.Common.Component.MangaListItem(
                            manga = item.manga,
                            subtitle = progressInfo,
                            isCompleted = item.history.isCompleted,
                            onPlayClick = { onAction(HistoryAction.ClickContinue(item.manga, item.history)) },
                            onClick = { onAction(HistoryAction.ClickManga(item.manga)) }
                        )
                    }
                }
            }
        }
    }
}
