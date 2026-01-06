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
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.component.CardType
import br.acerola.manga.common.component.SmartCard
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto

// TODO: Criar string para tudo
fun LazyListScope.settingsSection(
    directory: MangaDirectoryDto,
    remoteInfo: MangaRemoteInfoDto?,
    mangaDirectoryViewModel: MangaDirectoryViewModel,
    mangaRemoteInfoViewModel: MangaRemoteInfoViewModel
) {
    item {
        SmartCard(
            type = CardType.CONTENT,
            title = "Cofigurções dos arquivos",
            modifier = Modifier.padding(all = 6.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
        ) {
            // TODO: Virar component
            SmartCard(
                type = CardType.CONTENT, colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ), elevation = CardDefaults.elevatedCardElevation(
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
                                contentDescription = "Prefencias",
                            )
                        }

                        Spacer(modifier = Modifier.width(width = 12.dp))

                        Text(
                            text = "Preferencias da pagina de mangás",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }

    item {
        Spacer(modifier = Modifier.height(height = 12.dp))
    }

    item {
        SmartCard(
            type = CardType.CONTENT,
            title = "Cofigurções dos arquivos",
            modifier = Modifier.padding(all = 6.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
        ) {
            // TODO: Virar component
            SmartCard(
                type = CardType.CONTENT,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp, pressedElevation = 12.dp)
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
                                contentDescription = "Sincronizar arquivos do mangá",
                            )
                        }

                        Spacer(modifier = Modifier.width(width = 12.dp))

                        Text(
                            text = "Sincronizar arquivos",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }


                    ListItem(
                        modifier = Modifier.clickable { mangaDirectoryViewModel.syncChaptersByMangaDirectory(folderId = directory.id) },
                        headlineContent = {
                            Text(text = "Sincronizar arquivos dos capítulos")
                        },
                        supportingContent = {
                            Text(text = "Sincroniza métadados de cada capitulo do mangá")
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

                    Spacer(modifier = Modifier.width(width = 12.dp))


                    ListItem(
                        modifier = Modifier.clickable {
                            /* TODO: Função de sincronizar dados do mangá */
                        },
                        headlineContent = {
                            Text(text = "Sincronizar cover e banner")
                        },
                        supportingContent = {
                            Text(text = "Vai buscar os cover e banners já baixados na pasta do mangá")
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
                }
            }
        }
    }

    item {
        Spacer(modifier = Modifier.height(height = 12.dp))
    }

    item {
        SmartCard(
            type = CardType.CONTENT,
            title = "Sincronizar com mangadex",
            modifier = Modifier.padding(all = 6.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
        ) {
            // TODO: Virar component
            SmartCard(
                type = CardType.CONTENT,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp, pressedElevation = 12.dp)
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
                                contentDescription = "Sincronizar arquivos do mangá",
                            )
                        }

                        Spacer(modifier = Modifier.width(width = 12.dp))

                        Text(
                            text = "Sincronizar arquivos",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    ListItem(
                        headlineContent = {
                            Text(text = "Sincronizar metádados do manga")
                        }, supportingContent = {
                            Text(text = "Sincronizar os metadados do manga")
                        }, modifier = Modifier.clickable {
                            /* TODO: Função de sincronizar dados do mangá */
                        }, leadingContent = {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward, contentDescription = null
                            )
                        }, colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.width(width = 12.dp))

                    // TODO: Criar string e criar lógica para não aparecer quando os dados vem do mangádex
                    if (remoteInfo != null) {
                        ListItem(
                            headlineContent = {
                                Text(text = "Sincronizar metadados dos capítulos")
                            }, supportingContent = {
                                Text(text = "Sincroniza metadados de cada capitulo do mangá, baseado na numeração do capitulo, só para mangadex")
                            }, modifier = Modifier.clickable {
                                mangaRemoteInfoViewModel.syncChaptersByMangaRemoteInfo(mangaId = remoteInfo.id!!)
                            }, leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward, contentDescription = null
                                )
                            }, colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    }
}