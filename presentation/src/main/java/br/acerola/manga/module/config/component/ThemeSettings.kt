package br.acerola.manga.module.config.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import br.acerola.manga.common.theme.*
import br.acerola.manga.common.viewmodel.theme.ThemeViewModel
import br.acerola.manga.presentation.R

@Composable
fun ThemeSettings(
    themeViewModel: ThemeViewModel
) {
    val useDynamicColor by themeViewModel.useDynamicColor.collectAsState()
    val context = LocalContext.current

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
            ThemeCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.title_settings_catppuccin_theme),
                subtitle = stringResource(R.string.subtitle_settings_catppuccin_theme),
                selected = !useDynamicColor,
                colors = listOf(Mauve, Pink, Sky),
                onClick = { themeViewModel.setDynamicColor(false) }
            )

            val dynamicPrimary = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(context).primary
            } else {
                MaterialTheme.colorScheme.primary
            }

            val dynamicSecondary = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(context).secondary
            } else {
                MaterialTheme.colorScheme.secondary
            }

            val dynamicTertiary = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(context).tertiary
            } else {
                MaterialTheme.colorScheme.tertiary
            }

            ThemeCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.title_settings_dynamic_color),
                subtitle = stringResource(R.string.subtitle_settings_dynamic_color),
                selected = useDynamicColor,
                colors = listOf(dynamicPrimary, dynamicSecondary, dynamicTertiary),
                onClick = { themeViewModel.setDynamicColor(true) }
            )
        }
    }
}

@Composable
fun ThemeCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    selected: Boolean,
    colors: List<Color>,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val backgroundColor =
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant.copy(
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
            // Preview Colors
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background circle
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(colors),
                            shape = CircleShape
                        )
                )

                // Small circles overlapping
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
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
