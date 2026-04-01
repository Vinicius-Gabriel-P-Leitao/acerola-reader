package br.acerola.manga.module.main.config.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.mapper.LanguageMapper
import br.acerola.manga.module.main.Main

@Composable
fun Main.Config.Component.LanguageSettings(
    selectedLanguage: String?,
    onLanguageSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Text(
                text = "Idioma de Metadados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        supportingContent = {
            Text(
                text = "Idioma padrão para busca e metadados",
                style = MaterialTheme.typography.bodySmall,
            )
        },
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                        contentDescription = null
                    )
                }
            }
        },
        trailingContent = {
            Box {
                TextButton(onClick = { expanded = true }) {
                    Text(
                        // FIXME: Tem que virar uma string.xml
                        text = if (selectedLanguage != null) stringResource(id = LanguageMapper.getLabelRes(selectedLanguage)) else "Selecionar",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    LanguageMapper.getAllCodes().forEach { code ->
                        DropdownMenuItem(
                            text = { Text(stringResource(id = LanguageMapper.getLabelRes(code))) },
                            onClick = {
                                expanded = false
                                onLanguageSelected(code)
                            }
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

