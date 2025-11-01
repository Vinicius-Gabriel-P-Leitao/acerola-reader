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
    secondary = Teal,
    tertiary = Pink,
    background = Base,
    surface = Mantle,
    onPrimary = Text,
    onSecondary = Base,
    onTertiary = Base
)

private val LightColorScheme = lightColorScheme(
    primary = Surface0,
    secondary = Sky,
    tertiary = Peach,
    background = Text,
    surface = Subtext0,
    onPrimary = Text,
    onSecondary = Base,
    onTertiary = Base
)

@Composable
fun AcerolaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true, content: @Composable () -> Unit
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