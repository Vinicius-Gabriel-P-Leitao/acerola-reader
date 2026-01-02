package br.acerola.manga.module.manga.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.component.CardType
import br.acerola.manga.common.component.SmartCard

fun LazyListScope.settingsSection() {
    item { SettingHeader("Leitura") }

    item {
        SmartCard(
            type = CardType.CONTENT,
            title = "Teste",
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
        ) {
            // TODO: Virar component
            SmartCard(
                type = CardType.CONTENT,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 8.dp, pressedElevation = 12.dp
                )
            ) {
                Text("Item de teste")
            }
        }
    }

    item {
        var isChecked by remember { mutableStateOf(true) }
        SettingSwitchItem(
            icon = Icons.Default.AutoFixHigh,
            title = "Melhoria de imagem",
            subtitle = "Aplica filtros para limpar páginas escaneadas",
            checked = isChecked,
            onCheckedChange = { isChecked = it }
        )
    }

    item { SettingHeader("Interface") }

    item {
        var sliderValue by remember { mutableFloatStateOf(0.7f) }
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = "Brilho da tela",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    item {
        SettingItem(
            icon = Icons.Default.Palette,
            title = "Tema do App",
            subtitle = "Escuro (OLED)"
        )
    }

    item { SettingHeader("Segurança e Dados") }

    item {
        SettingItem(
            icon = Icons.Default.Storage,
            title = "Local de armazenamento",
            subtitle = "/Internal Storage/MangaApp/Media"
        )
    }

    item {
        SettingItem(
            icon = Icons.Default.DeleteForever,
            title = "Limpar todos os capítulos",
            subtitle = "Remover 1.2GB de arquivos baixados",
            titleColor = Color(0xFFE57373) // Vermelho suave
        )
    }

    item { Spacer(modifier = Modifier.height(40.dp)) }
}

// --- COMPONENTES AUXILIARES (UI) ---

@Composable
fun SettingHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = Color(0xFFD0BCFF), // Roxo claro Material3
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit = {}
) {
    ListItem(
        headlineContent = { Text(title, color = titleColor) },
        supportingContent = { subtitle?.let { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun SettingSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { subtitle?.let { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}