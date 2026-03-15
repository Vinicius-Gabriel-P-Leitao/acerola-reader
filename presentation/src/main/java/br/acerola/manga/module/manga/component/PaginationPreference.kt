package br.acerola.manga.module.manga.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.RadioGroup
import br.acerola.manga.config.preference.ChapterPageSizeType
import br.acerola.manga.module.manga.Manga
import br.acerola.manga.module.manga.MangaViewModel
import br.acerola.manga.presentation.R

@Composable
fun Manga.Component.PaginationPreference(
    selected: ChapterPageSizeType?,
    onSelect: (ChapterPageSizeType) -> Unit,
) {
    val options = ChapterPageSizeType.entries
    val selectedIndex = options.indexOf(selected).takeIf { it >= 0 } ?: 0

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
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

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = stringResource(id = R.string.title_settings_chapters_per_page),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(id = R.string.description_settings_chapters_per_page),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Acerola.Component.RadioGroup(
            selectedIndex = selectedIndex,
            options = options.map { it.key.lowercase() },
            onSelect = { index ->
                onSelect(options[index])
            }
        )
    }
}
