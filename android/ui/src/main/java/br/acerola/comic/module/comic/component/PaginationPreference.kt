package br.acerola.comic.module.comic.component
import br.acerola.comic.ui.R

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.RadioGroup
import br.acerola.comic.config.preference.ChapterPageSizeType
import br.acerola.comic.module.comic.Comic

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.graphics.Color

@Composable
fun Comic.Component.PaginationPreference(
    selected: ChapterPageSizeType?,
    onSelect: (ChapterPageSizeType) -> Unit,
) {
    val options = ChapterPageSizeType.entries
    val selectedIndex = options.indexOf(selected).takeIf { it >= 0 } ?: 0

    Column {
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.title_settings_chapters_per_page),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(id = R.string.description_settings_chapters_per_page),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            leadingContent = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = null
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Acerola.Component.RadioGroup(
                selectedIndex = selectedIndex,
                options = options.map { it.key.lowercase() },
                onSelect = { index ->
                    onSelect(options[index])
                }
            )
        }
    }
}
