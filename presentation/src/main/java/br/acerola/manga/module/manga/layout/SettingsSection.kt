package br.acerola.manga.module.manga.layout

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.component.CardType
import br.acerola.manga.common.component.SmartCard
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.presentation.R

fun LazyListScope.settingsSection(
    directory: MangaDirectoryDto,
    remoteInfo: MangaRemoteInfoDto?,
    mangaDirectoryViewModel: MangaDirectoryViewModel,
    mangaRemoteInfoViewModel: MangaRemoteInfoViewModel
) {
    // SEÇÃO: CONFIGURAÇÕES DE EXIBIÇÃO
    item {
        SmartCard(
            type = CardType.CONTENT,
            title = "Configurações de Exibição",
            modifier = Modifier.padding(all = 6.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            SmartCard(
                type = CardType.CONTENT,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(size = 40.dp)
                            .clip(CircleShape)
                            .background(color = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsSuggest,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(size = 22.dp),
                            contentDescription = null,
                        )
                    }
                    Spacer(modifier = Modifier.width(width = 12.dp))
                    Text(
                        text = "Preferências da página",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }

    item { Spacer(modifier = Modifier.height(height = 12.dp)) }

    item {
        SmartCard(
            type = CardType.CONTENT,
            title = "Configurações dos Arquivos",
            modifier = Modifier.padding(all = 6.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            SmartCard(
                type = CardType.CONTENT,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(size = 40.dp)
                                .clip(CircleShape)
                                .background(color = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderZip,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(size = 22.dp),
                                contentDescription = null,
                            )
                        }

                        Spacer(modifier = Modifier.width(width = 12.dp))

                        Text(
                            text = "Sincronizar arquivos",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    ListItem(
                        modifier = Modifier.clickable { mangaDirectoryViewModel.syncChaptersByMangaDirectory(folderId = directory.id) },
                        headlineContent = { Text(text = "Sincronizar capítulos") },
                        supportingContent = { Text(text = "Sincroniza metadados de cada capítulo local") },
                        leadingContent = { Icon(imageVector = Icons.Default.Refresh, contentDescription = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    ListItem(
                        modifier = Modifier.clickable { /* TODO */ },
                        headlineContent = { Text(text = "Sincronizar cover e banner") },
                        supportingContent = { Text(text = "Busca imagens já baixadas na pasta") },
                        leadingContent = { Icon(imageVector = Icons.Default.ImageSearch, contentDescription = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }

    item { Spacer(modifier = Modifier.height(12.dp)) }

    // SEÇÃO: MANGADEX (MANTIDO O ÍCONE DE IMAGEM)
    item {
        SmartCard(
            type = CardType.CONTENT,
            title = "Sincronizar com MangaDex",
            modifier = Modifier.padding(all = 6.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            SmartCard(
                type = CardType.CONTENT,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(size = 40.dp)
                                .clip(CircleShape)
                                .background(color = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.mangadex_v2),
                                contentDescription = stringResource(id = R.string.description_icon_sync_mangadex),
                                modifier = Modifier.size(size = 30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(width = 12.dp))

                        Text(
                            text = stringResource(id = R.string.title_config_sync_mangadex),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(text = stringResource(id = R.string.title_sync_mangadex_remote_info)) },
                        modifier = Modifier.clickable {
                            remoteInfo?.id?.let { id ->
                                mangaRemoteInfoViewModel.rescanMangaByManga(mangaId = id)
                            }
                        },
                        supportingContent = {
                            Text(
                                text = pluralStringResource(
                                    id = R.plurals.description_sync_mangadex_remote_info_supporting,
                                    count = 1
                                )
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null
                            )
                        },
                    )

                    if (remoteInfo != null) {
                        ListItem(
                            headlineContent = { Text(text = "Sincronizar capítulos") },
                            supportingContent = { Text(text = "Sincroniza numeração oficial e datas") },
                            modifier = Modifier.clickable {
                                mangaRemoteInfoViewModel.syncChaptersByManga(mangaId = remoteInfo.id!!)
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = null
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}