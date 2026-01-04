package br.acerola.manga.module.manga.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.component.CardType
import br.acerola.manga.common.component.Divider
import br.acerola.manga.common.component.SmartCard
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.feature.R

fun LazyListScope.settingsSection(
    directory: MangaDirectoryDto,
    remoteInfo: MangaRemoteInfoDto?,
    mangaDirectoryViewModel: MangaDirectoryViewModel,
    mangaRemoteInfoViewModel: MangaRemoteInfoViewModel
) {
    item { SettingHeader(title = "Leitura") }

    item {
        SmartCard(
            type = CardType.CONTENT,
            title = "Teste",
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
        ) {
            // TODO: Virar component
            SmartCard(
                type = CardType.CONTENT,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 8.dp, pressedElevation = 12.dp
                )
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(size = 40.dp)
                                .clip(CircleShape)
                                .background(color = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(size = 22.dp),
                                contentDescription = stringResource(
                                    id = R.string.description_icon_sync_manga_directory
                                ),
                            )
                        }

                        Spacer(modifier = Modifier.width(width = 12.dp))

                        Text(
                            text = stringResource(id = R.string.title_config_sync_modal),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Divider()

                    ListItem(
                        modifier = Modifier.clickable { mangaDirectoryViewModel.syncChaptersByMangaDirectory(folderId = directory.id) },
                        headlineContent = {
                            Text(text = stringResource(id = R.string.description_text_home_quick_sync))
                        },
                        supportingContent = {
                            Text(text = stringResource(id = R.string.description_text_home_quick_sync_supporting))
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Sync, contentDescription = null
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )

                    HorizontalDivider()

                    if (remoteInfo != null) {
                        ListItem(
                            modifier = Modifier.clickable {
                                mangaRemoteInfoViewModel.syncChaptersByMangaRemoteInfo(mangaId = remoteInfo.id!!)
                            },
                            headlineContent = { Text(text = stringResource(id = R.string.title_sync_remote_info)) },
                            supportingContent = { Text(text = stringResource(id = R.string.description_sync_remote_info_supporting)) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward, contentDescription = null
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    }

    item {
        var isChecked by remember { mutableStateOf(true) }
        SettingSwitchItem(
            icon = Icons.Default.AutoFixHigh,
            title = "Melhoria de imagem",
            subtitle = "Aplica filtros para limpar páginas escaneadas",
            checked = isChecked,
            onCheckedChange = { isChecked = it }
        )
    }

    item { SettingHeader("Interface") }

    item {
        var sliderValue by remember { mutableFloatStateOf(0.7f) }
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = "Brilho da tela",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    item {
        SettingItem(
            icon = Icons.Default.Palette,
            title = "Tema do App",
            subtitle = "Escuro (OLED)"
        )
    }

    item { SettingHeader("Segurança e Dados") }

    item {
        SettingItem(
            icon = Icons.Default.Storage,
            title = "Local de armazenamento",
            subtitle = "/Internal Storage/MangaApp/Media"
        )
    }

    item {
        SettingItem(
            icon = Icons.Default.DeleteForever,
            title = "Limpar todos os capítulos",
            subtitle = "Remover 1.2GB de arquivos baixados",
            titleColor = Color(0xFFE57373) // Vermelho suave
        )
    }

    item { Spacer(modifier = Modifier.height(40.dp)) }
}

@Composable
fun SettingHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = Color(0xFFD0BCFF), // Roxo claro Material3
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit = {}
) {
    ListItem(
        headlineContent = { Text(title, color = titleColor) },
        supportingContent = { subtitle?.let { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun SettingSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { subtitle?.let { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}