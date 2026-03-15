package br.acerola.manga.common.theme

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
    primary = CatppuccinMocha.Mauve,
    onPrimary = CatppuccinMocha.Base,
    primaryContainer = CatppuccinMocha.Surface2,
    onPrimaryContainer = CatppuccinMocha.Mauve,
    secondary = CatppuccinMocha.Pink,
    onSecondary = CatppuccinMocha.Base,
    secondaryContainer = CatppuccinMocha.Surface2,
    onSecondaryContainer = CatppuccinMocha.Pink,
    tertiary = CatppuccinMocha.Sky,
    onTertiary = CatppuccinMocha.Base,
    tertiaryContainer = CatppuccinMocha.Surface2,
    onTertiaryContainer = CatppuccinMocha.Sky,
    background = CatppuccinMocha.Base,
    onBackground = CatppuccinMocha.Text,
    surface = CatppuccinMocha.Surface0,
    onSurface = CatppuccinMocha.Text,
    surfaceVariant = CatppuccinMocha.Surface1,
    onSurfaceVariant = CatppuccinMocha.Subtext1,
    outline = CatppuccinMocha.Overlay0,
    error = CatppuccinMocha.Red,
    onError = CatppuccinMocha.Base,
    errorContainer = CatppuccinMocha.Maroon,
    onErrorContainer = CatppuccinMocha.Text
)

private val LightColorScheme = lightColorScheme(
    primary = CatppuccinLatte.Mauve,
    onPrimary = CatppuccinLatte.Base,
    primaryContainer = CatppuccinLatte.Surface2,
    onPrimaryContainer = CatppuccinLatte.Mauve,
    secondary = CatppuccinLatte.Pink,
    onSecondary = CatppuccinLatte.Base,
    secondaryContainer = CatppuccinLatte.Surface2,
    onSecondaryContainer = CatppuccinLatte.Pink,
    tertiary = CatppuccinLatte.Sky,
    onTertiary = CatppuccinLatte.Base,
    tertiaryContainer = CatppuccinLatte.Surface2,
    onTertiaryContainer = CatppuccinLatte.Sky,
    background = CatppuccinLatte.Base,
    onBackground = CatppuccinLatte.Text,
    surface = CatppuccinLatte.Surface0,
    onSurface = CatppuccinLatte.Text,
    surfaceVariant = CatppuccinLatte.Surface1,
    onSurfaceVariant = CatppuccinLatte.Subtext1,
    outline = CatppuccinLatte.Overlay0,
    error = CatppuccinLatte.Red,
    onError = CatppuccinLatte.Base,
    errorContainer = CatppuccinLatte.Maroon,
    onErrorContainer = CatppuccinLatte.Text
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

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme, typography = Typography, content = content
    )
}
