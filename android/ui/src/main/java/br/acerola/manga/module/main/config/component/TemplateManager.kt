package br.acerola.manga.module.main.config.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsSuggest
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
import br.acerola.manga.module.main.Main
import br.acerola.manga.ui.R

@Composable
fun Main.Config.Component.TemplateManager(
    onManageTemplates: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = stringResource(id = R.string.title_chapter_naming_templates),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        supportingContent = {
            Text(
                text = stringResource(id = R.string.description_template_config_activity),
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SettingsSuggest,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentDescription = null
                    )
                }
            }
        },
        modifier = Modifier.clickable { onManageTemplates() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
