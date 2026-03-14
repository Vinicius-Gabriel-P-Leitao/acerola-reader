package br.acerola.manga.module.reader.layout

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.component.AcerolaGlassButton
import br.acerola.manga.infrastructure.logging.AcerolaLogger
import br.acerola.manga.infrastructure.logging.LogSource
import br.acerola.manga.presentation.R

@Composable
fun ReaderBottomControls(
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
                .clip(RoundedCornerShape(32.dp))
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(25.dp)
                        } else {
                            Modifier
                        }
                    )
                    .background(glassColor)
                    .border(0.5.dp, borderColor, RoundedCornerShape(32.dp))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (hasPreviousChapter) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (isLoading) 0.05f else 0.1f))
                                    .clickable(enabled = !isLoading, onClick = onPreviousChapterClick)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                                        contentDescription = null,
                                        tint = if (isLoading) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(id = R.string.label_reader_previous_chapter),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isLoading) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (enableNavigation) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AcerolaGlassButton(
                                modifier = Modifier.size(40.dp),
                                onClick = onPrevClick
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = stringResource(id = R.string.description_icon_pagination_previous),
                                    tint = if (currentPage > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.3f
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            AcerolaGlassButton(
                                modifier = Modifier.size(40.dp),
                                onClick = onNextClick
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = stringResource(id = R.string.description_icon_pagination_next),
                                    tint = if (currentPage < pageCount - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.3f
                                    )
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        if (isChapterRead && hasNextChapter) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isLoading) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary)
                                    .clickable(enabled = !isLoading, onClick = onNextChapterClick)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(id = R.string.label_reader_next_chapter),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = if (isLoading) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onPrimary,
                                        maxLines = 1,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                        contentDescription = null,
                                        tint = if (isLoading) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val TAG = "ReaderBottomControls"
