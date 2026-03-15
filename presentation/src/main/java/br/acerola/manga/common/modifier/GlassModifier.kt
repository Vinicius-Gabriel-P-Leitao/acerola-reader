package br.acerola.manga.common.modifier

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Aplica o estilo de vidro (Glassmorphism) a um componente.
 * Desfoca o fundo, aplica uma cor semitransparente e uma borda suave.
 */
fun Modifier.glassStyle(
    shape: Shape,
    glassColor: Color,
    borderColor: Color
): Modifier = this.then(
    Modifier
        .clip(shape)
        .then(
            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                Modifier.blur(20.dp)
            } else Modifier
        )
        .background(glassColor)
        .border(0.5.dp, borderColor, shape)
)
