package br.acerola.manga.common.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ButtonType {
    ICON, TEXT, ICON_TEXT, GLASS_ICON
}

@Composable
fun AcerolaButton(
    type: ButtonType,
    onClick: () -> Unit,
    text: String? = null,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
) {
    when (type) {
        ButtonType.ICON -> {
            require(icon != null) { "IconButton precisa de um ícone" }
            IconButton(onClick = onClick, modifier = modifier) {
                icon()
            }
        }

        ButtonType.TEXT -> {
            require(text != null) { "TextButton precisa de texto" }
            Button(onClick = onClick, modifier = modifier) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text)
                }
            }
        }

        ButtonType.ICON_TEXT -> {
            require(icon != null && text != null) { "Button com ícone + texto precisa de ambos" }
            Button(onClick = onClick, modifier = modifier) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    icon()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text)
                }
            }
        }

        ButtonType.GLASS_ICON -> {
            require(icon != null) { "GlassButton precisa de um ícone" }
            AcerolaGlassButton(onClick = onClick, modifier = modifier) {
                icon()
            }
        }
    }
}

@Composable
fun AcerolaGlassButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val glassColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(20.dp)
                    } else {
                        Modifier
                    }
                )
                .background(glassColor)
                .border(0.5.dp, borderColor, CircleShape)
        )

        Box(
            modifier = Modifier.matchParentSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
