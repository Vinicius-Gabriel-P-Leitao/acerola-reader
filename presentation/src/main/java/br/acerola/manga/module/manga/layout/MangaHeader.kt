package br.acerola.manga.module.manga.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.component.ButtonType
import br.acerola.manga.common.component.SmartButton
import br.acerola.manga.dto.MangaDto
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun MangaHeader(
    manga: MangaDto, textColor: Color, secondaryTextColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = 420.dp)
    ) {
        val bannerModel = manga.directory.bannerUri ?: manga.directory.coverUri

        AsyncImage(
            contentDescription = null,
            contentScale = ContentScale.Crop,
            model = ImageRequest.Builder(context = LocalContext.current)
                .data(data = bannerModel)
                .crossfade(enable = true).build(),
            modifier = Modifier
                .fillMaxWidth()
                .height(height = 300.dp)
                .blur(radius = 10.dp)
                .align(Alignment.TopCenter)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = 300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent, MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom
            ) {
                AsyncImage(
                    contentDescription = "Cover",
                    contentScale = ContentScale.Crop,
                    model = ImageRequest.Builder(context = LocalContext.current)
                        .data(data = manga.directory.coverUri)
                        .crossfade(enable = true)
                        .build(),
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(size = 12.dp))
                        .width(width = 130.dp)
                        .background(Color.Gray)
                        .height(height = 190.dp)
                )

                Spacer(modifier = Modifier.width(width = 16.dp))

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .height(height = 170.dp)
                        .weight(weight = 1f),
                ) {
                    Text(
                        text = manga.remoteInfo?.title ?: manga.directory.name,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold, color = textColor
                        ),
                    )

                    Spacer(modifier = Modifier.height(height = 4.dp))

                    Text(
                        text = manga.remoteInfo?.status ?: "Unknown",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryTextColor
                    )

                    Spacer(modifier = Modifier.height(height = 8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Create,
                            modifier = Modifier.size(size = 16.dp),
                            tint = Color(color = 0xFFFFC107),
                            contentDescription = null,
                        )

                        Spacer(modifier = Modifier.width(width = 4.dp))

                        Text(
                            text = manga.remoteInfo?.authors?.name ?: "Unknown",
                            style = MaterialTheme.typography.labelLarge,
                            color = textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(height = 20.dp))

            // TODO: Criar função para pegar o ultimo antes de marcado como lido.
            SmartButton(
                text = "Continue",
                type = ButtonType.TEXT,
                onClick = { /* TODO: Ação Continuar */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 50.dp),
            )
        }
    }
}