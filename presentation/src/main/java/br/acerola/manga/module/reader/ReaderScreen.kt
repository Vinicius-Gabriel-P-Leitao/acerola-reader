package br.acerola.manga.module.reader

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import br.acerola.manga.dto.archive.ChapterFileDto


// TODO: Fazer UI 100% mais bonita e otimizada com funções
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    chapter: ChapterFileDto?
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(chapter) {
        chapter?.let { viewModel.openChapter(it) }
    }

    LazyColumn {
        items(state.pageCount) { index ->
            state.pages[index]?.let { bytes ->
                ReaderPage(
                    pageBytes = bytes,
                    modifier = Modifier.fillMaxWidth()
                )
            } ?: Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )

            LaunchedEffect(index) {
                viewModel.onPageVisible(index)
            }
        }
    }
}

@Composable
fun ReaderPage(
    pageBytes: ByteArray, modifier: Modifier = Modifier
) {
    val imageBitmap = remember(pageBytes) {
        BitmapFactory.decodeByteArray(
            pageBytes, 0, pageBytes.size
        )?.asImageBitmap()
    }

    if (imageBitmap == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f), contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Image(
        bitmap = imageBitmap,
        contentDescription = null,
        modifier = modifier.fillMaxWidth(),
        contentScale = ContentScale.Fit
    )
}
