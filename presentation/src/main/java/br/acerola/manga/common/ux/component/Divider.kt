package br.acerola.manga.common.ux.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.ux.Acerola

@Composable
fun Acerola.Component.Divider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.padding(vertical = 8.dp)
    )
}
