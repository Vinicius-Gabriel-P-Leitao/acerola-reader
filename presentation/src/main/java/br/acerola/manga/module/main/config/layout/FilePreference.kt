package br.acerola.manga.module.main.config.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.RadioGroup
import br.acerola.manga.config.preference.FileExtension
import br.acerola.manga.module.main.Main

@Composable
fun Main.Config.Layout.FilePreference(
    selected: FileExtension,
    onSelect: (FileExtension) -> Unit
) {
    val options = FileExtension.entries
    val selectedIndex = options.indexOf(selected).takeIf { it >= 0 } ?: 0

    Column {
        Acerola.Component.RadioGroup(
            selectedIndex = selectedIndex,
            options = options.map { it.extension.lowercase() },
            onSelect = { index ->
                onSelect(options[index])
            }
        )
    }
}
