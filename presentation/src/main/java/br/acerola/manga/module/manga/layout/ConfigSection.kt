package br.acerola.manga.module.manga.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.component.CardType
import br.acerola.manga.common.component.SmartCard
import br.acerola.manga.common.viewmodel.library.archive.ChapterArchiveViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.ChapterRemoteInfoViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.module.manga.MangaViewModel
import br.acerola.manga.module.manga.component.ConfigPreferences
import br.acerola.manga.module.manga.component.SyncMangaArchive
import br.acerola.manga.module.manga.component.SyncMangadexData
import br.acerola.manga.presentation.R

fun LazyListScope.configSection(
    directory: MangaDirectoryDto,
    remoteInfo: MangaRemoteInfoDto?,
    mangaViewModel: MangaViewModel,
    mangaDirectoryViewModel: MangaDirectoryViewModel,
    chapterArchiveViewModel: ChapterArchiveViewModel,
    mangaRemoteInfoViewModel: MangaRemoteInfoViewModel,
    chapterRemoteInfoViewModel: ChapterRemoteInfoViewModel,
) {
    item { Spacer(modifier = Modifier.height(24.dp)) }

    // --- Section: Display Settings (Visualização) ---
    item {
        PrettyConfigCard(
            title = stringResource(id = R.string.title_settings_display_config),
            icon = Icons.Rounded.Visibility,
            iconColor = MaterialTheme.colorScheme.primary
        ) {
            ConfigPreferences(mangaViewModel = mangaViewModel)
        }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }

    // --- Section: File Management (Armazenamento) ---
    item {
        PrettyConfigCard(
            title = stringResource(id = R.string.title_settings_file_config),
            icon = Icons.Rounded.SdStorage,
            iconColor = MaterialTheme.colorScheme.secondary // Pink no seu tema
        ) {
            SyncMangaArchive(
                directory = directory,
                mangaDirectoryViewModel = mangaDirectoryViewModel,
                chapterArchiveViewModel = chapterArchiveViewModel,
            )
        }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }

    // --- Section: Remote Sync (Nuvem) ---
    item {
        PrettyConfigCard(
            title = stringResource(id = R.string.title_config_sync_mangadex),
            icon = Icons.Rounded.CloudSync,
            iconColor = MaterialTheme.colorScheme.tertiary // Sky no seu tema
        ) {
            SyncMangadexData(
                directory = directory,
                remoteInfo = remoteInfo,
                mangaViewModel = mangaViewModel,
                mangaRemoteInfoViewModel = mangaRemoteInfoViewModel,
                chapterRemoteInfoViewModel = chapterRemoteInfoViewModel
            )
        }
    }

    item { Spacer(modifier = Modifier.height(48.dp)) }
}

/**
 * Wrapper visual local para manter o código do LazyListScope limpo e bonito.
 * Coloca o Ícone dentro de um container colorido arredondado.
 */
@androidx.compose.runtime.Composable
private fun PrettyConfigCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    content: @androidx.compose.runtime.Composable () -> Unit
) {
    SmartCard(
        type = CardType.CONTENT,
        title = null,
        modifier = Modifier.padding(horizontal = 16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            // Cabeçalho Bonito
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            content()
        }
    }
}