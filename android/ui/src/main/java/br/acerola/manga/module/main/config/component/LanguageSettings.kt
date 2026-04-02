package br.acerola.manga.module.main.config.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import br.acerola.manga.common.mapper.LanguageMapper
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.layout.LanguageSelector
import br.acerola.manga.module.main.Main
import br.acerola.manga.pattern.LanguagePattern
import br.acerola.manga.ui.R

@Composable
fun Main.Config.Component.LanguageSettings(
    selectedLanguage: String?,
    onLanguageSelected: (String) -> Unit
) {
    Acerola.Layout.LanguageSelector(
        selectedLanguage = selectedLanguage ?: LanguagePattern.PT_BR.code,
        onLanguageSelected = onLanguageSelected,
        trigger = { onClick ->
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.title_settings_metadata_language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                supportingContent = {
                    Text(
                        text = stringResource(id = R.string.description_settings_metadata_language),
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
                    Text(
                        text = if (selectedLanguage.isNullOrBlank()) stringResource(id = R.string.label_select_language) 
                               else stringResource(id = LanguageMapper.getLabelRes(selectedLanguage)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.clickable { onClick() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    )
}
