package br.acerola.comic.common.ux.component
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.tokens.SpacingTokens

@Composable
fun Acerola.Component.ImageCard(
    image: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
) {
    ElevatedCard(
        colors = colors,
        onClick = onClick,
        modifier = modifier,
        elevation = elevation,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = image,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            if (footer != null) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = SpacingTokens.Medium, vertical = SpacingTokens.Small),
                ) {
                    Text(
                        text = footer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
fun Acerola.Component.TextCard(
    text: String,
    title: String? = null,
    footer: String? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
) {
    ElevatedCard(
        colors = colors,
        onClick = onClick,
        modifier = modifier,
        elevation = elevation,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(all = SpacingTokens.Large),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(SpacingTokens.Small))
            }

            Text(text = text, style = MaterialTheme.typography.bodyMedium)

            if (footer != null) {
                Spacer(modifier = Modifier.height(SpacingTokens.Large))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(SpacingTokens.Large))
                Text(
                    text = footer,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
fun Acerola.Component.Card(
    onClick: () -> Unit = {},
    title: String? = null,
    footer: String? = null,
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        colors = colors,
        onClick = onClick,
        modifier = modifier,
        elevation = elevation,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(all = SpacingTokens.Large),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(SpacingTokens.Small))
            }

            content()

            if (footer != null) {
                Spacer(modifier = Modifier.height(SpacingTokens.Large))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(SpacingTokens.Large))
                Text(
                    text = footer,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
