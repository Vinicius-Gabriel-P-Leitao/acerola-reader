package br.acerola.manga.ui.common.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.acerola.manga.R

enum class CardType {
    IMAGE, TEXT, CONTENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCard(
    type: CardType,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    title: String? = null,
    footer: String? = null,
    image: Painter? = null,
    text: String? = null,
    content: @Composable () -> Unit = {},
) {
    val context = LocalContext.current

    ElevatedCard(
        colors = colors,
        onClick = onClick,
        modifier = modifier,
        elevation = elevation,
    ) {
        when (type) {
            CardType.IMAGE -> {
                // TODO: Criar exception personalizada para isso no código.
                requireNotNull(value = image) { context.getString(R.string.message_image_parameter_required_smart_card) }

                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = image,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (footer != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = footer,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            CardType.TEXT -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp)
                ) {
                    if (title !== null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(height = 8.dp))
                    }

                    if (text !== null) {
                        Text(text = text, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (footer !== null) {
                        Spacer(modifier = Modifier.height(height = 16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(height = 16.dp))
                        Text(
                            text = footer,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }

            CardType.CONTENT -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp)
                ) {
                    if (title !== null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(height = 8.dp))
                    }

                    content()

                    if (footer !== null) {
                        Spacer(modifier = Modifier.height(height = 16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(height = 16.dp))
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
    }
}