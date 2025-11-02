package br.acerola.manga.ui.common.theme

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
    primary = Overlay0,
    onPrimary = Text,
    secondary = Teal,
    onSecondary = Base,
    tertiary = Pink,
    onTertiary = Base,
    background = Base,
    onBackground = Text,
    surface = Mantle,
    onSurface = Text,
    error = Red,
    onError = Base
)

private val LightColorScheme = lightColorScheme(
    primary = Surface,
    onPrimary = Text,
    secondary = Sapphire,
    onSecondary = Base,
    tertiary = Peach,
    onTertiary = Base,
    background = Text,
    onBackground = Base,
    surface = Subtext,
    onSurface = Base,
    error = Red,
    onError = Base
)

@Composable
fun AcerolaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = false, content: @Composable () -> Unit
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