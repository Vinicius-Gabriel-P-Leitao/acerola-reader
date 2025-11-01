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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class CardType {
    IMAGE, CONTENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCard(
    type: CardType,
    title: String,
    imagePainter: Painter? = null,
    contentText: String? = null,
    footerText: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
    ) {
        when (type) {
            CardType.IMAGE -> {
                requireNotNull(value = imagePainter) { "imagePainter não pode ser nulo para CardType.IMAGE" }

                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = imagePainter,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (footerText != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = footerText, style = MaterialTheme.typography.bodyMedium, color = Color.White
                            )
                        }
                    }
                }
            }

            CardType.CONTENT -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp)
                ) {
                    Text(
                        text = title, style = MaterialTheme.typography.titleLarge
                    )

                    if (contentText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = contentText, style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (footerText != null) {
                        Spacer(modifier = Modifier.height(height = 16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(height = 16.dp))
                        Text(
                            text = footerText,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}