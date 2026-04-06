package br.acerola.comic.common.ux.component
import br.acerola.comic.ui.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import br.acerola.comic.common.ux.Acerola

data class FloatingToolItem(
    val icon: @Composable () -> Unit,
    val label: String? = null,
    val onClick: () -> Unit
)

@Composable
fun Acerola.Component.FloatingTool(
    icon: @Composable () -> Unit,
    items: List<FloatingToolItem>,
    modifier: Modifier = Modifier,
    paddingFromEdges: Dp = 16.dp,
    spacingBetweenItems: Dp = 14.dp
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd
    ) {

        Column(
            horizontalAlignment = Alignment.End, modifier = Modifier.padding(paddingFromEdges),
            verticalArrangement = Arrangement.spacedBy(spacingBetweenItems)
        ) {

            LazyColumn(
                reverseLayout = true,
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(spacingBetweenItems),
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .graphicsLayer { clip = false }
                    .padding(end = 4.dp),
            ) {
                itemsIndexed(
                    items = items.reversed(),
                    key = { _, item -> item.label ?: item.hashCode().toString() }) { index, item ->

                    val enterDelay = index * 50
                    val exitDelay = (items.size - 1 - index) * 30
                    val itemZIndex = (items.size - index).toFloat()

                    AnimatedVisibility(
                        visible = expanded,
                        modifier = Modifier.zIndex(itemZIndex).graphicsLayer { clip = false },
                        enter = slideInHorizontally(
                            animationSpec = tween(delayMillis = enterDelay), initialOffsetX = { it / 2 }) + fadeIn(
                            animationSpec = tween(delayMillis = enterDelay)
                        ), exit = slideOutHorizontally(
                            animationSpec = tween(delayMillis = exitDelay), targetOffsetX = { it / 2 }) + fadeOut(
                            animationSpec = tween(delayMillis = exitDelay)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.graphicsLayer { clip = false }) {
                            item.label?.let { label ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                    tonalElevation = 2.dp, modifier = Modifier.clickable {
                                        item.onClick()
                                        expanded = false
                                    }) {
                                    Text(
                                        text = label, style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp, vertical = 4.dp
                                        ), color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            FloatingActionButton(
                                modifier = Modifier.size(48.dp), containerColor = MaterialTheme.colorScheme.secondary,
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = 0.dp, pressedElevation = 0.dp, focusedElevation = 0.dp,
                                    hoveredElevation = 0.dp
                                ), onClick = {
                                    item.onClick()
                                    expanded = false
                                }) {
                                item.icon()
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                modifier = Modifier.size(56.dp), containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp, pressedElevation = 8.dp
                ), onClick = { expanded = !expanded }) {
                icon()
            }
        }
    }
}
