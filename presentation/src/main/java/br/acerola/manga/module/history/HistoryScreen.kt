package br.acerola.manga.module.history

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.manga.module.home.component.MangaListItem
import br.acerola.manga.module.manga.MangaActivity

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel
) {
    val historyItems by viewModel.historyItems.collectAsState()
    val context = LocalContext.current

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
                text = "Histórico de Leitura",
                modifier = Modifier.padding(vertical = 16.dp),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
            )

            if (historyItems.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Nenhum mangá lido recentemente",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(historyItems, key = { it.manga.directory.id }) { item ->
                        val chapterInfo = item.history.chapterName ?: "Capítulo desconhecido"
                        val progressInfo = "Capítulo $chapterInfo - Pág. ${item.history.lastPage + 1}"
                        
                        MangaListItem(
                            manga = item.manga,
                            subtitle = progressInfo,
                            isCompleted = item.history.isCompleted,
                            onPlayClick = {
                                val intent = Intent(context, br.acerola.manga.module.reader.ReaderActivity::class.java).apply {
                                    putExtra(br.acerola.manga.module.reader.ReaderActivity.PageExtra.MANGA_ID, item.manga.directory.id)
                                    putExtra(br.acerola.manga.module.reader.ReaderActivity.PageExtra.CHAPTER_ID, item.history.chapterArchiveId)
                                    putExtra(br.acerola.manga.module.reader.ReaderActivity.PageExtra.INITIAL_PAGE, item.history.lastPage)
                                }
                                context.startActivity(intent)
                            },
                            onClick = {
                                val intent = Intent(context, MangaActivity::class.java).apply {
                                    putExtra(MangaActivity.ChapterExtra.MANGA, item.manga)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}