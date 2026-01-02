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
    primary = Mauve,
    onPrimary = Base,
    primaryContainer = Surface2,
    onPrimaryContainer = Mauve,
    secondary = Pink,
    onSecondary = Base,
    secondaryContainer = Surface2,
    onSecondaryContainer = Pink,
    tertiary = Sky,
    onTertiary = Base,
    tertiaryContainer = Surface2,
    onTertiaryContainer = Sky,
    background = Base,
    onBackground = Text,
    surface = Surface0,
    onSurface = Text,
    surfaceVariant = Surface1,
    onSurfaceVariant = Subtext1,
    outline = Overlay0,
    error = Red,
    onError = Base,
    errorContainer = Maroon,
    onErrorContainer = Text
)

private val LightColorScheme = lightColorScheme(
    primary = Mauve,
    onPrimary = Base,
    secondary = Pink,
    onSecondary = Base,
    tertiary = Sky,
    onTertiary = Base,
    background = Text,
    onBackground = Base,
    surface = Subtext1,
    onSurface = Base,
    error = Red,
    onError = Base
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
