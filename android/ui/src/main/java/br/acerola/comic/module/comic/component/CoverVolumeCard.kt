package br.acerola.comic.module.comic.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.GlassButton
import br.acerola.comic.common.ux.component.GroupedHeroItem
import br.acerola.comic.dto.archive.VolumeChapterGroupDto
import br.acerola.comic.module.comic.Comic
import br.acerola.comic.ui.R
import coil.compose.AsyncImage

@Composable
fun Comic.Component.CoverVolumeCard(
    group: VolumeChapterGroupDto,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Acerola.Component.GroupedHeroItem(
        title = group.volume.name,
        description = stringResource(R.string.label_volume_card_description, group.loadedCount, group.totalChapters),
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
        icon = {
            if (group.volume.coverUri != null) {
                AsyncImage(
                    model = group.volume.coverUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.LibraryBooks,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        },
        nestedItem = {}
    )
}
