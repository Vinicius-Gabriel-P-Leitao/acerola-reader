package br.acerola.manga.module.main.config.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.Dialog
import br.acerola.manga.common.ux.component.DialogButton
import br.acerola.manga.local.entity.archive.ChapterTemplate
import br.acerola.manga.module.main.Main
import br.acerola.manga.ui.R

@Composable
fun Main.Config.Component.TemplateManager(
    templates: List<ChapterTemplate>,
    onAddTemplate: (String, String) -> Unit,
    onDeleteTemplate: (Long) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    
    val visibleTemplates = if (expanded) templates else templates.take(5)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.title_chapter_naming_templates),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.action_add_template))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        visibleTemplates.forEach { template ->
            TemplateItem(
                template = template,
                onDelete = { onDeleteTemplate(template.id) }
            )
        }

        if (templates.size > 5) {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = if (expanded) 
                        stringResource(R.string.label_reader_mode_vertical).uppercase() // FIXME: Texto sem sentido
                        else stringResource(R.string.label_settings_see_more_themes)
                )
            }
        }
    }

    if (showDialog) {
        AddTemplateDialog(
            onDismiss = { showDialog = false },
            onConfirm = { label, pattern ->
                onAddTemplate(label, pattern)
                showDialog = false
            }
        )
    }
}

@Composable
private fun TemplateItem(
    template: ChapterTemplate,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = template.pattern,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!template.isDefault) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(id = R.string.description_icon_delete_template),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Text(
                    text = stringResource(id = R.string.label_system_template),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AddTemplateDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("") }

    Acerola.Component.Dialog(
        show = true,
        onDismiss = onDismiss,
        title = stringResource(id = R.string.title_dialog_new_template),
        confirmButtonContent = {
            Acerola.Component.DialogButton(
                text = stringResource(id = R.string.action_add),
                onClick = { if (label.isNotBlank() && pattern.isNotBlank()) onConfirm(label, pattern) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        dismissButtonContent = {
            Acerola.Component.DialogButton(
                text = stringResource(id = R.string.action_cancel),
                onClick = onDismiss,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        content = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(id = R.string.label_template_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text(stringResource(id = R.string.label_template_pattern)) },
                    placeholder = { Text(stringResource(id = R.string.placeholder_template_pattern)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(id = R.string.description_template_macros),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    )
}
