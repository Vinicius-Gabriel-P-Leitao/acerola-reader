package br.acerola.manga.module.main.config.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.Button
import br.acerola.manga.common.ux.component.Dialog
import br.acerola.manga.common.ux.theme.color.Alucard
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
    var showThemeDialog by remember { mutableStateOf(false) }

    // Determina qual tema estático exibir no primeiro card
    val staticThemeToDisplay = if (currentTheme == AppTheme.DYNAMIC) AppTheme.CATPPUCCIN else currentTheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
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

            Spacer(modifier = Modifier.width(width = 12.dp))

            Column {
                Text(
                    text = stringResource(R.string.title_settings_appearance),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    text = stringResource(R.string.description_settings_appearance)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1: Tema Atual (ou Catppuccin se estiver em Adaptável)
            ThemeCard(
                modifier = Modifier.weight(1f),
                title = getThemeTitle(staticThemeToDisplay),
                subtitle = getThemeSubtitle(staticThemeToDisplay),
                selected = currentTheme == staticThemeToDisplay,
                colors = getThemeColors(staticThemeToDisplay, isDark, context),
                onClick = { onThemeChange(staticThemeToDisplay) }
            )

            // Card 2: Tema Adaptável
            ThemeCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.title_settings_dynamic_color),
                subtitle = stringResource(R.string.subtitle_settings_dynamic_color),
                selected = currentTheme == AppTheme.DYNAMIC,
                colors = dynamicColorsFromContext(context, isDark),
                onClick = { onThemeChange(AppTheme.DYNAMIC) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Acerola.Component.Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showThemeDialog = true },
            text = stringResource(id = R.string.label_settings_see_more_themes),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }

    if (showThemeDialog) {
        Acerola.Component.Dialog(
            show = showThemeDialog,
            onDismiss = { showThemeDialog = false },
            title = stringResource(id = R.string.title_dialog_select_theme),
            confirmButtonContent = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(id = R.string.label_dialog_close))
                }
            }
        ) {
            val themes = AppTheme.entries

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(300.dp)
            ) {
                items(themes) { theme ->
                    ThemeCard(
                        title = getThemeTitle(theme),
                        subtitle = getThemeSubtitle(theme),
                        selected = currentTheme == theme,
                        colors = getThemeColors(theme, isDark, context),
                        onClick = {
                            onThemeChange(theme)
                            showThemeDialog = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun getThemeTitle(theme: AppTheme): String {
    return when (theme) {
        AppTheme.CATPPUCCIN -> stringResource(R.string.title_settings_catppuccin_theme)
        AppTheme.NORD -> stringResource(R.string.title_settings_nord_theme)
        AppTheme.DRACULA -> stringResource(R.string.title_settings_dracula_theme)
        AppTheme.DYNAMIC -> stringResource(R.string.title_settings_dynamic_color)
    }
}

@Composable
private fun getThemeSubtitle(theme: AppTheme): String {
    return when (theme) {
        AppTheme.CATPPUCCIN -> stringResource(R.string.subtitle_settings_catppuccin_theme)
        AppTheme.NORD -> stringResource(R.string.subtitle_settings_nord_theme)
        AppTheme.DRACULA -> stringResource(R.string.subtitle_settings_dracula_theme)
        AppTheme.DYNAMIC -> stringResource(R.string.subtitle_settings_dynamic_color)
    }
}

@Composable
private fun getThemeColors(theme: AppTheme, isDark: Boolean, context: android.content.Context): List<Color> {
    return when (theme) {
        AppTheme.CATPPUCCIN -> listOf(CatppuccinMocha.Mauve, CatppuccinMocha.Pink, CatppuccinMocha.Sky)
        AppTheme.NORD -> if (isDark) listOf(NordDark.Primary, NordDark.Secondary, NordDark.Tertiary) else listOf(NordLight.Primary, NordLight.Secondary, NordLight.Tertiary)
        AppTheme.DRACULA -> if (isDark) listOf(Dracula.Purple, Dracula.Pink, Dracula.Cyan) else listOf(Alucard.Purple, Alucard.Pink, Alucard.Cyan)
        AppTheme.DYNAMIC -> dynamicColorsFromContext(context, isDark)
    }
}

@Composable
private fun dynamicColorsFromContext(context: android.content.Context, isDark: Boolean): List<Color> {
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
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val backgroundColor =
        if (selected) MaterialTheme.colorScheme.primary.copy(
            alpha = 0.05f
        ) else MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0.3f
        )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
        color = backgroundColor,
        tonalElevation = if (selected) 4.dp else 0.dp
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
