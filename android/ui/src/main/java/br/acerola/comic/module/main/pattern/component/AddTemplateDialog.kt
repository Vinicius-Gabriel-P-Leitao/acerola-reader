package br.acerola.comic.module.main.pattern.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.AdaptiveSheet
import br.acerola.comic.module.main.Main
import br.acerola.comic.ui.R

@Composable
fun Main.Pattern.Component.AddTemplateDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    initialLabel: String = "",
    initialPattern: String = "",
    isEditMode: Boolean = false,
) {
    var label by remember { mutableStateOf(initialLabel) }
    var pattern by remember { mutableStateOf(initialPattern) }

    Acerola.Component.AdaptiveSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(
                text =
                    stringResource(
                        id =
                            if (isEditMode) {
                                R.string.title_dialog_edit_template
                            } else {
                                R.string.title_dialog_new_template
                            },
                    ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(id = R.string.label_template_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = pattern,
                onValueChange = { pattern = it },
                label = { Text(stringResource(id = R.string.label_template_pattern)) },
                placeholder = { Text(stringResource(id = R.string.placeholder_template_pattern)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.description_template_macros),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(id = R.string.action_cancel))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { if (label.isNotBlank() && pattern.isNotBlank()) onConfirm(label, pattern) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text =
                            stringResource(
                                id = if (isEditMode) R.string.action_save else R.string.action_add,
                            ),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
