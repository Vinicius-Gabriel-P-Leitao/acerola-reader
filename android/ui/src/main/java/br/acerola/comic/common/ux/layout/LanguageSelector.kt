package br.acerola.comic.common.ux.layout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.mapper.LanguageMapper
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.AdaptiveSheet

@Composable
fun Acerola.Layout.LanguageSelector(
    selectedLanguage: String?,
    onLanguageSelected: (String) -> Unit,
    trigger: @Composable (onClick: () -> Unit) -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }

    trigger { showSheet = true }

    if (showSheet) {
        Acerola.Component.AdaptiveSheet(
            onDismissRequest = { showSheet = false },
        ) {
            LazyColumn(modifier = Modifier.padding(bottom = 32.dp)) {
                items(LanguageMapper.getAllCodes()) { code ->
                    ListItem(
                        headlineContent = { Text(stringResource(id = LanguageMapper.getLabelRes(code))) },
                        leadingContent = {
                            RadioButton(
                                selected = code == selectedLanguage,
                                onClick = null,
                            )
                        },
                        modifier =
                            Modifier.clickable {
                                onLanguageSelected(code)
                                showSheet = false
                            },
                    )
                }
            }
        }
    }
}
