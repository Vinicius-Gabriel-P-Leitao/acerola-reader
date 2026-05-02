package br.acerola.comic.common.ux.component
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.modifier.glass
import br.acerola.comic.common.ux.modifier.glassContainer

@Composable
fun Acerola.Component.TopBar(
    title: String? = null,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            navigationIcon?.invoke()
        }

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (title != null) {
                TitleCapsule(text = title)
            }
        }

        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            actions?.invoke()
        }
    }
}

@Composable
fun Acerola.Component.TitleCapsule(
    text: String,
    modifier: Modifier = Modifier,
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val glassColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .glassContainer(shape),
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .glass(shape, glassColor, borderColor),
        )

        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
