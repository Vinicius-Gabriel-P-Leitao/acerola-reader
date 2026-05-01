package br.acerola.comic.module.comic.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.HeroItem
import br.acerola.comic.common.ux.component.RadioGroup
import br.acerola.comic.config.preference.types.ChapterPageSizeType
import br.acerola.comic.module.comic.Comic
import br.acerola.comic.ui.R

@Composable
fun Comic.Component.PaginationPreference(
    selected: ChapterPageSizeType?,
    onSelect: (ChapterPageSizeType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = ChapterPageSizeType.entries
    val selectedIndex = options.indexOf(selected).takeIf { it >= 0 } ?: 0

    Acerola.Component.HeroItem(
        title = stringResource(id = R.string.title_settings_chapters_per_page),
        description = selected?.key?.lowercase(),
        icon = Icons.Default.AutoStories,
        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
        iconBackground = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier,
        bottomContent = {
            Acerola.Component.RadioGroup(
                selectedIndex = selectedIndex,
                options = options.map { it.key.lowercase() },
                onSelect = { index -> onSelect(options[index]) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        },
    )
}
