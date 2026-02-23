package br.acerola.manga.module.config.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.component.CardType
import br.acerola.manga.common.component.Divider
import br.acerola.manga.common.component.SmartCard
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.presentation.R

@Composable
fun SyncMangadexData(
    mangaDexViewModel: MangaRemoteInfoViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "MangaDex",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        ListItem(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { mangaDexViewModel.rescanMangas() },
            headlineContent = { Text(text = stringResource(id = R.string.title_sync_mangadex_remote_info)) },
            supportingContent = {
                Text(
                    text = pluralStringResource(
                        id = R.plurals.description_sync_mangadex_remote_info_supporting,
                        count = 2
                    )
                )
            },
            leadingContent = {
                Image(
                    painter = painterResource(id = R.drawable.mangadex_v2),
                    contentDescription = null,
                    modifier = Modifier.size(size = 24.dp)
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}
