package br.acerola.manga.common.ux.layout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.mapper.LanguageMapper
import br.acerola.manga.common.ux.Acerola

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Acerola.Layout.LanguageSelector(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    trigger: @Composable (onClick: () -> Unit) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    trigger { showSheet = true }

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
