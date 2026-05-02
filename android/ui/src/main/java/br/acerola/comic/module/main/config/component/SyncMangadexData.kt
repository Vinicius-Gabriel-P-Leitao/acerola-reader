package br.acerola.comic.module.main.config.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.HeroButton
import br.acerola.comic.module.main.Main
import br.acerola.comic.ui.R

@Composable
fun Main.Config.Component.SyncMangadexData(
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Acerola.Component.HeroButton(
        title = stringResource(id = R.string.title_sync_mangadex_remote_info),
        description = pluralStringResource(id = R.plurals.description_sync_mangadex_remote_info_supporting, count = 2),
        iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
        onClick = onRescan,
        modifier = modifier,
        icon = {
            Image(
                painter = painterResource(id = R.drawable.mangadex_v2),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
        },
    )
}
