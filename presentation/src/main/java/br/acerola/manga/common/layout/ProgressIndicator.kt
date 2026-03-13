package br.acerola.manga.common.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.manga.presentation.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    isLoading: Boolean,
) {
    var showIndicator by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    // Gerencia a visibilidade e o estado de conclusão
    LaunchedEffect(isLoading) {
        if (isLoading) {
            isFinished = false
            showIndicator = true
        } else {
            if (showIndicator) {
                isFinished = true
                delay(2000) // Tempo para o usuário ver o "Check"
                showIndicator = false
                isFinished = false
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
                    // Animação de troca entre Loading e Check
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
                                tint = br.acerola.manga.common.theme.Green,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            if (progress == null) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(size = 24.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                CircularProgressIndicator(
                                    progress = { progress.coerceIn(0f, 1f) },
                                    modifier = Modifier.size(size = 24.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    }

                    // Animação de troca de Texto
                    AnimatedContent(
                        targetState = isFinished,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith
                                    fadeOut(animationSpec = tween(300))
                        },
                        label = "SyncStatusText"
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
                            color = if (finished) br.acerola.manga.common.theme.Green else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
