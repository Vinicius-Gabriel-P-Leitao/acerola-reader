package br.acerola.manga.ui.common.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(delayMillis = 150)) + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut(animationSpec = tween(delayMillis = 150)) + slideOutVertically(targetOffsetY = { it / 3 })
            ) {
                LazyColumn(
                    reverseLayout = true,
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(space = spacingBetweenItems)
                ) {
                    items(items.reversed()) { item ->
                        FloatingActionButton(
                            onClick = {
                                item.onClick()
                                expanded = false
                            },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(size = 48.dp)
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