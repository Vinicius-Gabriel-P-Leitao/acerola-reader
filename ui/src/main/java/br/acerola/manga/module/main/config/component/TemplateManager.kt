package br.acerola.manga.module.main.config.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
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

        templates.forEach { template ->
            TemplateItem(
                template = template,
                onDelete = { onDeleteTemplate(template.id) }
            )
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.title_dialog_new_template)) },
        text = {
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
                Text(
                    text = stringResource(id = R.string.description_template_macros),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(label, pattern) },
                enabled = label.isNotBlank() && pattern.isNotBlank()
            ) {
                Text(stringResource(id = R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.action_cancel))
            }
        }
    )
}
