package br.acerola.manga.module.main.config.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import br.acerola.manga.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main.Config.Component.LanguageSettings(
    selectedLanguage: String?,
    onLanguageSelected: (String) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

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
                text = if (selectedLanguage != null) stringResource(id = LanguageMapper.getLabelRes(selectedLanguage)) else stringResource(R.string.label_select_language),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable { showSheet = true },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            LazyColumn(modifier = Modifier.padding(bottom = 32.dp)) {
                items(LanguageMapper.getAllCodes()) { code ->
                    ListItem(
                        headlineContent = { Text(stringResource(id = LanguageMapper.getLabelRes(code))) },
                        leadingContent = {
                            RadioButton(
                                selected = code == selectedLanguage,
                                onClick = null
                            )
                        },
                        modifier = Modifier.clickable {
                            onLanguageSelected(code)
                            showSheet = false
                        }
                    )
                }
            }
        }
    }
}

