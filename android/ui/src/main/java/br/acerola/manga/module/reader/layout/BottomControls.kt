package br.acerola.manga.module.reader.layout

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.GlassButton
import br.acerola.manga.common.ux.modifier.glass
import br.acerola.manga.common.ux.modifier.glassContainer
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.module.reader.Reader
import br.acerola.manga.ui.R

private const val TAG = "ReaderBottomControls"

@Composable
fun Reader.Layout.BottomControls(
    pageCount: Int,
    currentPage: Int,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onNextChapterClick: () -> Unit,
    onPreviousChapterClick: () -> Unit,
    isChapterRead: Boolean = false,
    hasNextChapter: Boolean = false,
    hasPreviousChapter: Boolean = false,
    enableNavigation: Boolean = true,
    isLoading: Boolean = false
) {
    AcerolaLogger.d(
        TAG,
        "Recomposed: isChapterRead=$isChapterRead, hasNextChapter=$hasNextChapter, page=$currentPage/$pageCount",
        LogSource.UI
    )

    val glassColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val shape = RoundedCornerShape(32.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassContainer(shape)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .glass(shape, glassColor, borderColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PageIndicator(currentPage = currentPage, pageCount = pageCount)

                Spacer(modifier = Modifier.height(16.dp))

                ChapterNavigation(
                    isLoading = isLoading,
                    enableNavigation = enableNavigation,
                    currentPage = currentPage,
                    pageCount = pageCount,
                    hasPreviousChapter = hasPreviousChapter,
                    hasNextChapter = hasNextChapter,
                    isChapterRead = isChapterRead,
                    onPreviousChapterClick = onPreviousChapterClick,
                    onPrevClick = onPrevClick,
                    onNextClick = onNextClick,
                    onNextChapterClick = onNextChapterClick
                )
            }
        }
    }
}

@Composable
private fun PageIndicator(
    currentPage: Int,
    pageCount: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = stringResource(id = R.string.label_reader_pages_prefix),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "${currentPage + 1} / $pageCount",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val progress = if (pageCount > 0) (currentPage + 1).toFloat() / pageCount.toFloat() else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        )
    }
}

@Composable
private fun ChapterNavigation(
    isLoading: Boolean,
    enableNavigation: Boolean,
    currentPage: Int,
    pageCount: Int,
    hasPreviousChapter: Boolean,
    hasNextChapter: Boolean,
    isChapterRead: Boolean,
    onPreviousChapterClick: () -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onNextChapterClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            PreviousChapterButton(
                visible = hasPreviousChapter,
                isLoading = isLoading,
                onClick = onPreviousChapterClick
            )
        }

        if (enableNavigation) {
            PageNavigationControls(
                currentPage = currentPage,
                pageCount = pageCount,
                onPrevClick = onPrevClick,
                onNextClick = onNextClick
            )
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            NextChapterButton(
                visible = isChapterRead && hasNextChapter,
                isLoading = isLoading,
                onClick = onNextChapterClick
            )
        }
    }
}

@Composable
private fun PreviousChapterButton(
    visible: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    if (visible) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = if (isLoading) 0.05f else 0.1f)
                )
                .clickable(enabled = !isLoading, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                    contentDescription = null,
                    tint = if (isLoading) MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.3f
                    ) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(id = R.string.label_reader_previous_chapter),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isLoading) MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.3f
                    ) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun NextChapterButton(
    visible: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    if (visible) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (isLoading) MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.5f
                    ) else MaterialTheme.colorScheme.primary
                )
                .clickable(enabled = !isLoading, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = R.string.label_reader_next_chapter),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = if (isLoading) MaterialTheme.colorScheme.onPrimary.copy(
                        alpha = 0.5f
                    ) else MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = null,
                    tint = if (isLoading) MaterialTheme.colorScheme.onPrimary.copy(
                        alpha = 0.5f
                    ) else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PageNavigationControls(
    currentPage: Int,
    pageCount: Int,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Acerola.Component.GlassButton(
            modifier = Modifier.size(40.dp),
            onClick = onPrevClick,
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(
                        id = R.string.description_icon_pagination_previous
                    ),
                    tint = if (currentPage > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.3f
                    )
                )
            }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Acerola.Component.GlassButton(
            modifier = Modifier.size(40.dp),
            onClick = onNextClick,
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(
                        id = R.string.description_icon_pagination_next
                    ),
                    tint = if (currentPage < pageCount - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.3f
                    )
                )
            }
        )
    }
}
