package br.acerola.manga.ui.feature.main.config.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import br.acerola.manga.shared.config.preference.FileExtension
import br.acerola.manga.ui.common.component.RadioGroup
import br.acerola.manga.ui.common.viewmodel.archive.file.FilePreferencesViewModel

@Composable
fun FilePreference(viewModel: FilePreferencesViewModel = viewModel()) {
    val selected by viewModel.selectedExtension.collectAsState(initial = null)
    val options = FileExtension.entries

    val selectedIndex = options.indexOf(selected).takeIf { it >= 0 } ?: 0

    Column {
        RadioGroup(
            selectedIndex = selectedIndex,
            options = options.map { it.extension.lowercase() },
            onSelect = { index ->
                val extension = options[index]
                viewModel.saveExtension(value = extension)
            }
        )
    }
}