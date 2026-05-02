package br.acerola.comic.module.comic.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.GlassButton
import br.acerola.comic.common.ux.component.GroupedHeroItem
import br.acerola.comic.dto.archive.VolumeChapterGroupDto
import br.acerola.comic.module.comic.Comic
import br.acerola.comic.ui.R

@Composable
fun Comic.Component.VolumeCard(
    group: VolumeChapterGroupDto,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Acerola.Component.GroupedHeroItem(
        title = group.volume.name,
        // FIXME: Trocar por texto label_volume_header_chapter_count, mas fazer plural.xml capítulos e capitulo
        description = stringResource(R.string.label_volume_card_description, group.loadedCount, group.totalChapters),
        icon = Icons.Default.LibraryBooks,
        iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
        iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier,
        onClick = onToggleExpanded,
        action = {
            Acerola.Component.GlassButton(
                onClick = onToggleExpanded,
                icon = {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                },
            )
        },
        nestedItem =
            if (group.volume.isSpecial) {
                {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(text = stringResource(R.string.label_volume_header_special)) },
                    )
                }
            } else {
                null
            },
    )
}
