package br.acerola.comic.module.main.common.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.AdaptiveSheet
import br.acerola.comic.common.ux.component.Dialog
import br.acerola.comic.common.ux.component.DialogButton
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.dto.metadata.category.CategoryDto
import br.acerola.comic.module.main.Main
import br.acerola.comic.ui.R
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun Main.Common.Component.ComicActionsSheet(
    manga: ComicDto,
    categories: List<CategoryDto>,
    onHide: () -> Unit,
    onDelete: () -> Unit,
    onBookmark: (categoryId: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var showCategorySheet by remember { mutableStateOf(false) }
    var showHideDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val title = manga.remoteInfo?.title ?: manga.directory.name
    val currentCategoryName = manga.category?.name

    val context = LocalContext.current
    val coverUri = manga.directory.coverUri ?: manga.directory.bannerUri

    Acerola.Component.AdaptiveSheet(
        onDismissRequest = onDismiss,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(data = coverUri)
                    .memoryCacheKey("${coverUri}_${manga.directory.lastModified}")
                    .diskCacheKey("${coverUri}_${manga.directory.lastModified}")
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .width(56.dp)
                    .height(84.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
                if (currentCategoryName != null) {
                    Text(
                        text = currentCategoryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        HorizontalDivider()

        ListItem(
            leadingContent = {
                Icon(
                    imageVector = if (manga.category != null) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = null,
                )
            },
            headlineContent = { Text(text = stringResource(id = R.string.action_bookmark)) },
            supportingContent = {
                Text(
                    text = currentCategoryName ?: stringResource(id = R.string.label_no_bookmark)
                )
            },
            modifier = Modifier.clickable { showCategorySheet = true },
        )

        ListItem(
            leadingContent = {
                Icon(
                    imageVector = Icons.Rounded.VisibilityOff,
                    contentDescription = null,
                )
            },
            headlineContent = { Text(text = stringResource(id = R.string.action_hide)) },
            supportingContent = { Text(text = stringResource(id = R.string.description_hide)) },
            modifier = Modifier.clickable { showHideDialog = true },
        )

        ListItem(
            leadingContent = {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.action_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(id = R.string.description_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            modifier = Modifier.clickable { showDeleteDialog = true },
        )

        Spacer(modifier = Modifier.navigationBarsPadding())
    }

    if (showCategorySheet) {
        ComicCategorySheet(
            categories = categories,
            selectedCategoryId = manga.category?.id,
            onSelect = { categoryId ->
                onBookmark(categoryId)
                showCategorySheet = false
                onDismiss()
            },
            onDismiss = { showCategorySheet = false },
        )
    }

    if (showHideDialog) {
        Acerola.Component.Dialog(
            show = true,
            onDismiss = { showHideDialog = false },
            title = stringResource(id = R.string.dialog_hide_title),
            confirmButtonContent = {
                Acerola.Component.DialogButton(
                    text = stringResource(id = R.string.action_hide),
                    onClick = {
                        showHideDialog = false
                        onHide()
                        onDismiss()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            dismissButtonContent = {
                Acerola.Component.DialogButton(
                    text = stringResource(id = R.string.action_cancel),
                    onClick = { showHideDialog = false },
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            content = { Text(text = stringResource(id = R.string.dialog_hide_message)) },
        )
    }

    if (showDeleteDialog) {
        Acerola.Component.Dialog(
            show = true,
            onDismiss = { showDeleteDialog = false },
            title = stringResource(id = R.string.dialog_delete_title),
            confirmButtonContent = {
                Acerola.Component.DialogButton(
                    text = stringResource(id = R.string.action_delete),
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                        onDismiss()
                    },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    fontWeight = FontWeight.Bold
                )
            },
            dismissButtonContent = {
                Acerola.Component.DialogButton(
                    text = stringResource(id = R.string.action_cancel),
                    onClick = { showDeleteDialog = false },
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            content = { Text(text = stringResource(id = R.string.dialog_delete_message)) },
        )
    }
}

@Composable
private fun ComicCategorySheet(
    categories: List<CategoryDto>,
    selectedCategoryId: Long?,
    onSelect: (categoryId: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    Acerola.Component.AdaptiveSheet(
        onDismissRequest = onDismiss,
    ) {
        Text(
            text = stringResource(id = R.string.action_bookmark),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )

        HorizontalDivider()

        LazyColumn {
            item {
                ListItem(
                    leadingContent = {
                        RadioButton(
                            selected = selectedCategoryId == null,
                            onClick = { onSelect(null) },
                        )
                    },
                    headlineContent = { Text(text = stringResource(id = R.string.action_remove_bookmark)) },
                    modifier = Modifier.clickable { onSelect(null) },
                )
            }

            items(items = categories) { category ->
                ListItem(
                    leadingContent = {
                        RadioButton(
                            selected = selectedCategoryId == category.id,
                            onClick = { onSelect(category.id) },
                        )
                    },
                    headlineContent = { Text(text = category.name) },
                    trailingContent = {
                        Spacer(
                            modifier = Modifier
                                .size(20.dp)
                                .drawBehind {
                                    drawCircle(color = Color(category.color))
                                }
                        )
                    },
                    modifier = Modifier.clickable { onSelect(category.id) },
                )
            }
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
