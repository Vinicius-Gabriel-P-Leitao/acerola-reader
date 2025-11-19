package br.acerola.manga.ui.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class ButtonType {
    ICON, TEXT, ICON_TEXT
}

@Composable
fun SmartButton(
    type: ButtonType,
    onClick: () -> Unit,
    text: String? = null,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
) {
    // TODO: Tratar erros melhor e gerar string para cada msg de erro.
    when (type) {
        ButtonType.ICON -> {
            require(value = icon != null) { "IconButton precisa de um ícone" }
            IconButton(onClick = onClick, modifier = modifier) {
                icon()
            }
        }

        ButtonType.TEXT -> {
            require(value = text != null) { "TextButton precisa de texto" }
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
            require(value = icon != null && text != null) { "Button com ícone + texto precisa de ambos" }
            Button(onClick = onClick, modifier = modifier) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    icon()
                    Spacer(modifier = Modifier.width(width = 8.dp))
                    Text(text)
                }
            }
        }
    }
}