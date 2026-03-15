package br.acerola.manga.common.ux.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Mauve,
    onPrimary = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Base,
    primaryContainer = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Surface2,
    onPrimaryContainer = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Mauve,
    secondary = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Pink,
    onSecondary = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Base,
    secondaryContainer = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Surface2,
    onSecondaryContainer = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Pink,
    tertiary = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Sky,
    onTertiary = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Base,
    tertiaryContainer = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Surface2,
    onTertiaryContainer = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Sky,
    background = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Base,
    onBackground = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Text,
    surface = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Surface0,
    onSurface = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Text,
    surfaceVariant = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Surface1,
    onSurfaceVariant = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Subtext1,
    outline = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Overlay0,
    error = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Red,
    onError = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Base,
    errorContainer = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Maroon,
    onErrorContainer = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinMocha.Text
)

private val LightColorScheme = lightColorScheme(
    primary = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Mauve,
    onPrimary = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Base,
    primaryContainer = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Surface2,
    onPrimaryContainer = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Mauve,
    secondary = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Pink,
    onSecondary = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Base,
    secondaryContainer = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Surface2,
    onSecondaryContainer = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Pink,
    tertiary = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Sky,
    onTertiary = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Base,
    tertiaryContainer = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Surface2,
    onTertiaryContainer = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Sky,
    background = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Base,
    onBackground = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Text,
    surface = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Surface0,
    onSurface = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Text,
    surfaceVariant = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Surface1,
    onSurfaceVariant = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Subtext1,
    outline = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Overlay0,
    error = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Red,
    onError = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Base,
    errorContainer = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Maroon,
    onErrorContainer = _root_ide_package_.br.acerola.manga.common.ux.theme.CatppuccinLatte.Text
)

@Composable
fun AcerolaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context: Context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> _root_ide_package_.br.acerola.manga.common.ux.theme.DarkColorScheme
        else -> _root_ide_package_.br.acerola.manga.common.ux.theme.LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme, typography = _root_ide_package_.br.acerola.manga.common.ux.theme.Typography, content = content
    )
}
