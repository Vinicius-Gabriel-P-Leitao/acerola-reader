package br.acerola.manga.common.ux.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarDefaults.InputField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.ui.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun <T> Acerola.Component.SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onBackClick: (() -> Unit)? = null,
    isLoading: Boolean = false,
    items: List<T>,
    placeholder: String,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 16.dp),
    itemContent: @Composable (T) -> Unit,
) {
    val internalBackClick = onBackClick ?: { onExpandedChange(false) }

    val animatedShape = rememberSearchBarShape(expanded)

    DockedSearchBar(
        modifier = modifier.animateContentSize(),
        inputField = {
            InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                placeholder = {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    if (expanded) {
                        IconButton(onClick = internalBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.label_search_back_to_results),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                trailingIcon = {
                    if (expanded && query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.description_icon_search_close),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            )
        },
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        shape = animatedShape,
        colors = SearchBarDefaults.colors(
            containerColor = if (expanded)
                MaterialTheme.colorScheme.surfaceContainerHigh
            else Color.Transparent,
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(150)) + expandVertically(),
            exit = fadeOut(tween(220)) + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column {
                    if (isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        )
                    } else {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }

                    if (items.isEmpty() && !isLoading && query.isNotEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.description_text_search_no_results),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        LazyColumn(contentPadding = contentPadding) {
                            items(
                                items = items,
                                key = { item -> itemKey(item) }
                            ) { item ->
                                itemContent(item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberSearchBarShape(expanded: Boolean): RoundedCornerShape {
    val transition = updateTransition(
        targetState = expanded,
        // FIXME: Transformar em string.xml
        label = "SearchBarTransition"
    )

    val bottomCornerRadius by transition.animateDp(
        transitionSpec = {
            if (targetState) {
                tween(
                    durationMillis = 200,
                    easing = FastOutSlowInEasing
                )
            } else {
                tween(
                    durationMillis = 300,
                    easing = LinearOutSlowInEasing
                )
            }
        },
        // FIXME: Transformar em string.xml
        label = "BottomCornerRadius"
    ) { isExpanded ->
        // 🔥 Aqui está a correção real
        if (isExpanded) 12.dp else 28.dp
    }

    return remember(bottomCornerRadius) {
        RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 28.dp,
            bottomStart = bottomCornerRadius,
            bottomEnd = bottomCornerRadius
        )
    }
}