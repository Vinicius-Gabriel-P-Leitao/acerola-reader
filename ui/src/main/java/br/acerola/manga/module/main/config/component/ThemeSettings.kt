package br.acerola.manga.module.main.config.component

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.acerola.manga.common.ux.theme.color.Alucard
import br.acerola.manga.common.ux.theme.color.CatppuccinLatte
import br.acerola.manga.common.ux.theme.color.CatppuccinMocha
import br.acerola.manga.common.ux.theme.color.Dracula
import br.acerola.manga.common.ux.theme.color.NordDark
import br.acerola.manga.common.ux.theme.color.NordLight
import br.acerola.manga.config.preference.AppTheme
import br.acerola.manga.module.main.Main
import br.acerola.manga.ui.R

@Composable
fun Main.Config.Component.ThemeSettings(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val themes = AppTheme.entries

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.title_settings_appearance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            supportingContent = {
                Text(
                    style = MaterialTheme.typography.bodySmall,
                    text = stringResource(R.string.description_settings_appearance)
                )
            },
            leadingContent = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                            contentDescription = null,
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(themes) { theme ->
                ThemeCard(
                    modifier = Modifier.width(150.dp),
                    title = getThemeTitle(theme, isDark),
                    subtitle = getThemeSubtitle(theme, isDark),
                    selected = currentTheme == theme,
                    colors = getThemeColors(theme, isDark, context),
                    onClick = { onThemeChange(theme) }
                )
            }
        }
    }
}

@Composable
private fun getThemeTitle(theme: AppTheme, isDark: Boolean): String {
    return when (theme) {
        AppTheme.CATPPUCCIN -> stringResource(R.string.title_settings_catppuccin_theme)
        AppTheme.NORD -> stringResource(R.string.title_settings_nord_theme)
        AppTheme.DRACULA -> if (isDark) stringResource(R.string.title_settings_dracula_theme) else stringResource(R.string.title_settings_alucard_theme)
        AppTheme.DYNAMIC -> stringResource(R.string.title_settings_dynamic_color)
    }
}

@Composable
private fun getThemeSubtitle(theme: AppTheme, isDark: Boolean): String {
    return when (theme) {
        AppTheme.CATPPUCCIN -> if (isDark) stringResource(R.string.subtitle_settings_mocha_theme) else stringResource(R.string.subtitle_settings_latte_theme)
        AppTheme.NORD -> if (isDark) stringResource(R.string.subtitle_settings_nord_dark_theme) else stringResource(R.string.subtitle_settings_nord_light_theme)
        AppTheme.DRACULA -> if (isDark) stringResource(R.string.subtitle_settings_vampire_theme) else stringResource(R.string.subtitle_settings_dracula_theme)
        AppTheme.DYNAMIC -> stringResource(R.string.subtitle_settings_dynamic_color)
    }
}

@Composable
private fun getThemeColors(
    theme: AppTheme,
    isDark: Boolean,
    context: Context
): List<Color> {
    return when (theme) {
        AppTheme.DYNAMIC -> dynamicColorsFromContext(context, isDark)
        AppTheme.CATPPUCCIN -> if (isDark) listOf(CatppuccinMocha.Mauve, CatppuccinMocha.Pink, CatppuccinMocha.Sky) else listOf(
            CatppuccinLatte.Mauve, CatppuccinLatte.Pink, CatppuccinLatte.Sky
        )
        AppTheme.DRACULA -> if (isDark) listOf(Dracula.Purple, Dracula.Pink, Dracula.Cyan) else listOf(
            Alucard.Purple, Alucard.Pink, Alucard.Cyan
        )
        AppTheme.NORD -> if (isDark) listOf(NordDark.Primary, NordDark.Secondary, NordDark.Tertiary) else listOf(
            NordLight.Primary, NordLight.Secondary, NordLight.Tertiary
        )
    }
}

@Composable
private fun dynamicColorsFromContext(
    context: Context,
    isDark: Boolean
): List<Color> {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val scheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        listOf(scheme.primary, scheme.secondary, scheme.tertiary)
    } else {
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun ThemeCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    selected: Boolean,
    colors: List<Color>,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor =
        if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface

    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(borderColor))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(colors),
                            shape = CircleShape
                        )
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy((-12).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(color, CircleShape)
                                .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(16.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
