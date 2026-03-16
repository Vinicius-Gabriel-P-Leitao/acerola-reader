package br.acerola.manga.common.ux.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.presentation.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Acerola.Layout.ProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    isLoading: Boolean,
) {
    var showIndicator by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            isFinished = false
            showIndicator = true
        } else {
            if (showIndicator) {
                delay(2000)
                isFinished = true
                isFinished = false
                showIndicator = false
            }
        }
    }

    AnimatedVisibility(
        visible = showIndicator,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.BottomStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedContent(
                        targetState = isFinished,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith
                                    fadeOut(animationSpec = tween(300))
                        },
                        label = "SyncStatusIcon"
                    ) { finished ->
                        if (finished) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            if (progress == null) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(size = 24.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                CircularProgressIndicator(
                                    progress = { progress.coerceIn(0f, 1f) },
                                    modifier = Modifier.size(size = 24.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = isFinished,
                        label = "SyncStatusText",
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith
                                    fadeOut(animationSpec = tween(300))
                        },
                    ) { finished ->
                        val text = if (finished) {
                            stringResource(id = R.string.label_sync_complete)
                        } else {
                            if (progress != null) {
                                stringResource(
                                    id = R.string.label_sync_progress_percent,
                                    (progress.coerceIn(0f, 1f) * 100).toInt()
                                )
                            } else {
                                stringResource(id = R.string.label_sync_progress)
                            }
                        }

                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (finished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
