package br.acerola.comic.module.main.config.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.HeroButton
import br.acerola.comic.module.main.Main
import br.acerola.comic.ui.R

@Composable
fun Main.Config.Component.MetadataExportSettings(
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Acerola.Component.HeroButton(
        title = stringResource(id = R.string.title_preference_metadata_comic_info),
        description = stringResource(id = R.string.description_preference_metadata_comic_info),
        icon = Icons.Filled.Description,
        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
        iconBackground = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier,
        action = {
            Switch(
                checked = enabled,
                onCheckedChange = onCheckedChange,
            )
        },
    )
}
