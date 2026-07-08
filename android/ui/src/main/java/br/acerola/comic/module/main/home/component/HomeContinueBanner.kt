package br.acerola.comic.module.main.home.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.acerola.comic.common.ux.tokens.SpacingTokens
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.dto.history.ReadingHistoryDto
import br.acerola.comic.module.main.Main
import br.acerola.comic.ui.R
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.roundToInt

private val BannerExpandedHeight = 280.dp
private val BannerCollapsedHeight = 72.dp
private val DragThreshold = 120f

@Composable
fun Main.Home.Component.HomeContinueBanner(
    comic: ComicDto,
    history: ReadingHistoryDto,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onContinueClick: () -> Unit,
    onComicClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var dragOffset by remember { mutableFloatStateOf(0f) }
    
    val coverUri = comic.directory.coverUri ?: comic.remoteInfo?.cover?.url
    val bannerUri = comic.directory.bannerUri ?: comic.remoteInfo?.banner?.url ?: coverUri
    val title = comic.remoteInfo?.title ?: comic.directory.name
    
    val targetHeight = if (isExpanded) BannerExpandedHeight.value else BannerCollapsedHeight.value
    val animatedHeight by animateFloatAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "banner_height"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeight.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { 
                if (!isExpanded) {
                    onExpandedChange(true)
                    return@clickable
                }
                onComicClick()
            }
            .pointerInput(isExpanded) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        when {
                            isExpanded && dragOffset < -DragThreshold -> onExpandedChange(false)
                            !isExpanded && dragOffset > DragThreshold -> onExpandedChange(true)
                        }
                        dragOffset = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        dragOffset = if (isExpanded) {
                            (dragOffset + dragAmount).coerceIn(-400f, 0f)
                        } else {
                            (dragOffset + dragAmount).coerceIn(0f, 400f)
                        }
                    }
                )
            }
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(animatedHeight.dp)) {
            if (isExpanded && bannerUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(bannerUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                                )
                            )
                        )
                )
            }
            
            if (!isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BannerCollapsedHeight)
                        .padding(horizontal = SpacingTokens.Large),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_banner_continue_reading).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(SpacingTokens.Medium))
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    FilledTonalButton(
                        onClick = onContinueClick,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BannerExpandedHeight)
                        .padding(SpacingTokens.Large)
                        .offset { IntOffset(0, (dragOffset * 0.5f).roundToInt()) },
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.label_banner_continue_reading).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(SpacingTokens.Medium))
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (bannerUri != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = stringResource(R.string.label_banner_chapter_progress, history.chapterSort),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = if (bannerUri != null) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1
                    )
                    
                    Spacer(modifier = Modifier.height(SpacingTokens.Large))
                    
                    FilledTonalButton(
                        onClick = onContinueClick,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(SpacingTokens.Small))
                        Text(
                            text = stringResource(R.string.label_banner_continue).uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isExpanded && bannerUri != null) {
                            Color.White.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        }
                    )
                    .size(width = 32.dp, height = 4.dp)
            )
        }
    }
}
