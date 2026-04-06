package br.acerola.comic.common.ux.modifier
import br.acerola.comic.ui.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

fun Modifier.glass(
    shape: Shape,
    glassColor: Color,
    borderColor: Color
): Modifier = this
    .blur(20.dp)
    .background(glassColor)
    .border(0.5.dp, borderColor, shape)

fun Modifier.glassContainer(shape: Shape): Modifier = this
    .clip(shape)
