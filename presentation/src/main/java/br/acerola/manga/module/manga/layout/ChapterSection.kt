package br.acerola.manga.module.manga.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.archive.ChapterPageDto
import br.acerola.manga.module.manga.component.ChapterItem

fun LazyListScope.chaptersSection(
    chapterPage: ChapterPageDto?,
    textColor: Color,
    onChapterClick: (ChapterFileDto) -> Unit,
    onLoadNextPage: () -> Unit
) {
    if (chapterPage == null) {
        item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        return
    }

    items(
        items = chapterPage.items,
        key = { it.id }
    ) { chapter ->
        ChapterListItem(
            chapter = chapter,
            textColor = textColor,
            onClick = { onChapterClick(chapter) }
        )
    }

    if (chapterPage.items.size < chapterPage.total) {
        item {
            LaunchedEffect(key1 = Unit) {
                onLoadNextPage()
            }

            Box(Modifier
                .fillMaxWidth()
                .padding(all = 16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(size = 24.dp))
            }
        }
    }
}

@Composable
fun ChapterListItem(
    chapter: ChapterFileDto, textColor: Color, onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(size = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        ChapterItem(chapter, textColor, onClick)
    }
}