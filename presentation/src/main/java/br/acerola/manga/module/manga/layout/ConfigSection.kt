package br.acerola.manga.module.manga.layout

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.Card
import br.acerola.manga.common.viewmodel.library.archive.ChapterArchiveViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.ChapterRemoteInfoViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.module.manga.Manga
import br.acerola.manga.module.manga.MangaViewModel
import br.acerola.manga.module.manga.component.PaginationPreference
import br.acerola.manga.module.manga.component.SyncMangaArchive
import br.acerola.manga.module.manga.component.SyncMetadata
import br.acerola.manga.presentation.R

fun Manga.Layout.ConfigSection(
    scope: LazyListScope,
    directory: MangaDirectoryDto,
    remoteInfo: MangaRemoteInfoDto?,
    mangaViewModel: MangaViewModel,
    mangaDirectoryViewModel: MangaDirectoryViewModel,
    chapterArchiveViewModel: ChapterArchiveViewModel,
    mangaRemoteInfoViewModel: MangaRemoteInfoViewModel,
    chapterRemoteInfoViewModel: ChapterRemoteInfoViewModel,
) {
    scope.item { Spacer(modifier = Modifier.height(24.dp)) }

    scope.item {
        PrettyConfigCard(
            title = stringResource(id = R.string.title_settings_display_config),
            icon = Icons.Rounded.Visibility,
            iconColor = MaterialTheme.colorScheme.primary
        ) {
            Manga.Component.PaginationPreference(mangaViewModel = mangaViewModel)
        }
    }

    scope.item { Spacer(modifier = Modifier.height(16.dp)) }

    scope.item {
        PrettyConfigCard(
            title = stringResource(id = R.string.title_text_archive_configs_in_app),
            icon = Icons.Rounded.SdStorage,
            iconColor = MaterialTheme.colorScheme.secondary
        ) {
            Manga.Component.SyncMangaArchive(
                directory = directory,
                mangaDirectoryViewModel = mangaDirectoryViewModel,
                chapterArchiveViewModel = chapterArchiveViewModel,
            )
        }
    }

    scope.item { Spacer(modifier = Modifier.height(16.dp)) }

    scope.item {
        PrettyConfigCard(
            title = stringResource(id = R.string.title_config_sync_mangadex),
            icon = Icons.Rounded.CloudSync,
            iconColor = MaterialTheme.colorScheme.tertiary
        ) {
            Manga.Component.SyncMetadata(
                directory = directory,
                remoteInfo = remoteInfo,
                mangaRemoteInfoViewModel = mangaRemoteInfoViewModel,
                chapterRemoteInfoViewModel = chapterRemoteInfoViewModel
            )
        }
    }

    scope.item { Spacer(modifier = Modifier.height(28.dp)) }
}

@Composable
private fun PrettyConfigCard(
    title: String,
    iconColor: Color,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Acerola.Component.Card(
        title = null,
        modifier = Modifier.padding(horizontal = 16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            content()
        }
    }
}
