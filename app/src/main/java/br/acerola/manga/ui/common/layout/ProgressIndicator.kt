package br.acerola.manga.ui.common.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun ProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    isLoading: Boolean,
) {
    if (!isLoading) return

    Box(
        contentAlignment = Alignment.BottomStart,
        modifier = modifier.fillMaxWidth()
    ) {
        Card(
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (progress == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(size = 24.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        strokeWidth = 3.dp
                    )
                } else {
                    CircularProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.size(size = 24.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        strokeWidth = 3.dp
                    )
                }

                val progressText = if (progress != null) {
                    "Sincronizando... ${(progress.coerceIn(0f, 1f) * 100).toInt()}%"
                } else {
                    "Sincronizando..."
                }

                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
