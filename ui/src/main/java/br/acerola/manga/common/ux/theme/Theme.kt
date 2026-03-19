package br.acerola.manga.common.ux.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import br.acerola.manga.common.ux.theme.color.Alucard
import br.acerola.manga.common.ux.theme.color.CatppuccinLatte
import br.acerola.manga.common.ux.theme.color.CatppuccinMocha
import br.acerola.manga.common.ux.theme.color.Dracula
import br.acerola.manga.common.ux.theme.color.NordDark
import br.acerola.manga.common.ux.theme.color.NordLight
import br.acerola.manga.config.preference.AppTheme

private val CatppuccinDarkColorScheme = darkColorScheme(
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

private val CatppuccinLightColorScheme = lightColorScheme(
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

private val NordDarkColorScheme = darkColorScheme(
    primary = NordDark.Primary,
    onPrimary = NordDark.Background,
    primaryContainer = NordDark.SurfaceVariant,
    onPrimaryContainer = NordDark.Primary,
    secondary = NordDark.Secondary,
    onSecondary = NordDark.Background,
    secondaryContainer = NordDark.SurfaceVariant,
    onSecondaryContainer = NordDark.Secondary,
    tertiary = NordDark.Tertiary,
    onTertiary = NordDark.Background,
    background = NordDark.Background,
    onBackground = NordDark.Text,
    surface = NordDark.Surface,
    onSurface = NordDark.Text,
    surfaceVariant = NordDark.SurfaceVariant,
    onSurfaceVariant = NordDark.Subtext,
    outline = NordDark.Outline,
    error = NordDark.Error,
    onError = NordDark.Text
)

private val NordLightColorScheme = lightColorScheme(
    primary = NordLight.Primary,
    onPrimary = NordLight.Background,
    primaryContainer = NordLight.SurfaceVariant,
    onPrimaryContainer = NordLight.Primary,
    secondary = NordLight.Secondary,
    onSecondary = NordLight.Background,
    secondaryContainer = NordLight.SurfaceVariant,
    onSecondaryContainer = NordLight.Secondary,
    tertiary = NordLight.Tertiary,
    onTertiary = NordLight.Background,
    background = NordLight.Background,
    onBackground = NordLight.Text,
    surface = NordLight.Surface,
    onSurface = NordLight.Text,
    surfaceVariant = NordLight.SurfaceVariant,
    onSurfaceVariant = NordLight.Subtext,
    outline = NordLight.Outline,
    error = NordLight.Error,
    onError = NordLight.Background
)

private val DraculaColorScheme = darkColorScheme(
    primary = Dracula.Purple,
    onPrimary = Dracula.Background,
    primaryContainer = Dracula.CurrentLine,
    onPrimaryContainer = Dracula.Purple,
    secondary = Dracula.Pink,
    onSecondary = Dracula.Background,
    secondaryContainer = Dracula.CurrentLine,
    onSecondaryContainer = Dracula.Pink,
    tertiary = Dracula.Cyan,
    onTertiary = Dracula.Background,
    background = Dracula.Background,
    onBackground = Dracula.Foreground,
    surface = Dracula.Background,
    onSurface = Dracula.Foreground,
    surfaceVariant = Dracula.CurrentLine,
    onSurfaceVariant = Dracula.Foreground,
    outline = Dracula.Comment,
    error = Dracula.Red,
    onError = Dracula.Foreground
)

private val AlucardColorScheme = lightColorScheme(
    primary = Alucard.Purple,
    onPrimary = Alucard.Background,
    primaryContainer = Alucard.CurrentLine,
    onPrimaryContainer = Alucard.Purple,
    secondary = Alucard.Pink,
    onSecondary = Alucard.Background,
    secondaryContainer = Alucard.CurrentLine,
    onSecondaryContainer = Alucard.Pink,
    tertiary = Alucard.Cyan,
    onTertiary = Alucard.Background,
    background = Alucard.Background,
    onBackground = Alucard.Foreground,
    surface = Alucard.Background,
    onSurface = Alucard.Foreground,
    surfaceVariant = Alucard.CurrentLine,
    onSurfaceVariant = Alucard.Foreground,
    outline = Alucard.Comment,
    error = Alucard.Red,
    onError = Alucard.Foreground
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun AcerolaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    theme: AppTheme = AppTheme.CATPPUCCIN,
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        AppTheme.DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context: Context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) CatppuccinDarkColorScheme else CatppuccinLightColorScheme
            }
        }
        AppTheme.NORD -> if (darkTheme) NordDarkColorScheme else NordLightColorScheme
        AppTheme.DRACULA -> if (darkTheme) DraculaColorScheme else AlucardColorScheme
        AppTheme.CATPPUCCIN -> if (darkTheme) CatppuccinDarkColorScheme else CatppuccinLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme, typography = Typography, content = content
    )
}
