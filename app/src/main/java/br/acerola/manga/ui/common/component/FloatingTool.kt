package br.acerola.manga.ui.common.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class FloatingToolItem(
    val icon: @Composable () -> Unit,
    val label: String? = null,
    val onClick: () -> Unit
)

@Composable
fun FloatingTool(
    icon: @Composable () -> Unit,
    items: List<FloatingToolItem>,
    modifier: Modifier = Modifier,
    paddingFromEdges: Dp = 16.dp,
    spacingBetweenItems: Dp = 12.dp,
) {
    var expanded by remember { mutableStateOf(value = false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            modifier = Modifier.padding(all = paddingFromEdges),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = spacingBetweenItems)
        ) {
            LazyColumn(
                reverseLayout = true,
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(space = spacingBetweenItems)
            ) {
                itemsIndexed(
                    items = items.reversed(),
                    key = { _, item -> item.label ?: item.hashCode() }
                ) { index, item ->
                    val enterDelay = index * 50
                    val exitDelay = (items.size - 1 - index) * 30

                    AnimatedVisibility(
                        visible = expanded,
                        enter = slideInHorizontally(
                            animationSpec = tween(delayMillis = enterDelay),
                            initialOffsetX = { it / 2 }
                        ) + fadeIn(animationSpec = tween(delayMillis = enterDelay)),
                        exit = slideOutHorizontally(
                            animationSpec = tween(delayMillis = exitDelay),
                            targetOffsetX = { it / 2 }
                        ) + fadeOut(animationSpec = tween(delayMillis = exitDelay))
                    ) {
                        FloatingActionButton(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(size = 48.dp),
                            onClick = {
                                item.onClick()
                                expanded = false
                            },
                        ) {
                            item.icon()
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.size(size = 56.dp),
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                icon()
            }
        }
    }
}