package br.acerola.comic.module.comic.component
import br.acerola.comic.ui.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.comic.dto.metadata.category.CategoryDto
import br.acerola.comic.module.comic.Comic

@Composable
fun Comic.Component.ComicCategorySelector(
    selectedCategory: CategoryDto?,
    allCategories: List<CategoryDto>,
    onUpdateMangaCategory: (Long?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (allCategories.isEmpty()) {
            Text(
                text = stringResource(id = R.string.label_category_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allCategories) { category ->
                    val isSelected = selectedCategory?.id == category.id
                    
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) {
                                onUpdateMangaCategory(null)
                            } else {
                                onUpdateMangaCategory(category.id)
                            }
                        },
                        label = {
                            Text(text = category.name)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Bookmark,
                                contentDescription = null,
                                tint = Color(category.color),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}
