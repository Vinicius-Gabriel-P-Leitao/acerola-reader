package br.acerola.manga.module.manga.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
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
import br.acerola.manga.module.manga.Manga
import br.acerola.manga.module.manga.component.MangaCategorySelector
import br.acerola.manga.module.manga.component.PaginationPreference
import br.acerola.manga.module.manga.component.SyncMangaArchive
import br.acerola.manga.module.manga.component.SyncMetadata
import br.acerola.manga.module.manga.state.MangaAction
import br.acerola.manga.module.manga.state.MangaSyncAction
import br.acerola.manga.module.manga.state.MangaUiState
import br.acerola.manga.ui.R

fun Manga.Layout.ConfigSection(
    scope: LazyListScope,
    uiState: MangaUiState,
    onAction: (MangaAction) -> Unit,
    onSyncAction: (MangaSyncAction) -> Unit,
) {
    scope.item { Spacer(modifier = Modifier.height(24.dp)) }

    scope.item {
        PrettyConfigCard(
            title = stringResource(id = R.string.title_settings_display_config),
            icon = Icons.Rounded.Visibility,
            iconColor = MaterialTheme.colorScheme.primary
        ) {
            Manga.Component.PaginationPreference(
                selected = uiState.selectedChapterPerPage,
                onSelect = { onAction(MangaAction.UpdatePageSize(it)) }
            )
        }
    }

    scope.item { Spacer(modifier = Modifier.height(16.dp)) }

    scope.item {
        PrettyConfigCard(
            title = stringResource(id = R.string.title_config_categories),
            icon = Icons.Rounded.Bookmark,
            iconColor = MaterialTheme.colorScheme.primary
        ) {
            Manga.Component.MangaCategorySelector(
                selectedCategory = uiState.manga.category,
                allCategories = uiState.allCategories,
                onUpdateMangaCategory = { id -> onAction(MangaAction.UpdateCategory(id)) }
            )
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
                onSyncChapters = { onSyncAction(MangaSyncAction.SyncChaptersLocal) },
                onRescanCover = { onSyncAction(MangaSyncAction.RescanManga) },
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
                remoteInfo = uiState.manga.remoteInfo,
                onSyncMangadexInfo = { onSyncAction(MangaSyncAction.SyncMangadexInfo) },
                onSyncMangadexChapters = { onSyncAction(MangaSyncAction.SyncMangadexChapters) },
                onSyncComicInfo = { onSyncAction(MangaSyncAction.SyncComicInfo) },
                onSyncComicInfoChapters = { onSyncAction(MangaSyncAction.SyncComicInfoChapters) },
                onSyncAnilistInfo = { onSyncAction(MangaSyncAction.SyncAnilistInfo) },
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            content()
        }
    }
}
