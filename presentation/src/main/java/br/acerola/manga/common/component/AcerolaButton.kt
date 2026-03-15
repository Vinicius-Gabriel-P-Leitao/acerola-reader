package br.acerola.manga.common.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.modifier.glassStyle

@Composable
fun Acerola.IconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        icon()
    }
}

@Composable
fun Acerola.Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null
) {
    Button(onClick = onClick, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text)
        }
    }
}

@Composable
fun Acerola.GlassButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = CircleShape,
        modifier = modifier
            .size(48.dp)
            .glassStyle(CircleShape, glassColor, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
    }
}
