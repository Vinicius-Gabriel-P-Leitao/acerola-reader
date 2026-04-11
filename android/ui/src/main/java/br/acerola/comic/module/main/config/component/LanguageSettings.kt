package br.acerola.comic.module.main.config.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import br.acerola.comic.common.mapper.LanguageMapper
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.HeroItem
import br.acerola.comic.common.ux.layout.LanguageSelector
import br.acerola.comic.module.main.Main
import br.acerola.comic.pattern.LanguagePattern
import br.acerola.comic.ui.R

@Composable
fun Main.Config.Component.LanguageSettings(
    selectedLanguage: String?,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val resolvedLanguage = selectedLanguage ?: LanguagePattern.PT_BR.code
    val languageLabel = stringResource(id = LanguageMapper.getLabelRes(resolvedLanguage))

    Acerola.Layout.LanguageSelector(
        selectedLanguage = resolvedLanguage,
        onLanguageSelected = onLanguageSelected,
        trigger = { onClick ->
            Acerola.Component.HeroItem(
                title = stringResource(id = R.string.title_settings_metadata_language),
                description = languageLabel,
                icon = Icons.Filled.Language,
                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                iconBackground = MaterialTheme.colorScheme.primaryContainer,
                modifier = modifier,
                action = {
                    IconButton(onClick = onClick) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(id = R.string.label_select_language),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    )
}
