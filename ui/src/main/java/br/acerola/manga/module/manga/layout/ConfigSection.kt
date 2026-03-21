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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import br.acerola.manga.common.ux.component.Divider
import androidx.compose.ui.draw.alpha
import br.acerola.manga.module.manga.Manga
import br.acerola.manga.module.manga.component.MangaCategorySelector
import br.acerola.manga.module.manga.component.PaginationPreference
import br.acerola.manga.module.manga.component.SyncMangaArchive
import br.acerola.manga.module.manga.component.SyncMetadata
import br.acerola.manga.module.manga.state.MangaAction
import br.acerola.manga.module.manga.state.MangaSyncAction
import br.acerola.manga.module.manga.state.MangaUiState
import br.acerola.manga.ui.R

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
    )
}

fun Manga.Layout.ConfigSection(
    scope: LazyListScope,
    uiState: MangaUiState,
    onAction: (MangaAction) -> Unit,
    onSyncAction: (MangaSyncAction) -> Unit
) {
    scope.item { Spacer(modifier = Modifier.height(16.dp)) }

    // NOTE: Configurações de Exibição
    scope.item {
        SectionHeader(stringResource(id = R.string.title_settings_display_config))
    }

    scope.item {
        Manga.Component.PaginationPreference(
            selected = uiState.selectedChapterPerPage,
            onSelect = { onAction(MangaAction.UpdatePageSize(it)) }
        )
    }

    scope.item {
        Acerola.Component.Divider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).alpha(0.3f))
    }

    // NOTE: Categorias
    scope.item {
        SectionHeader(stringResource(id = R.string.title_config_categories))
    }

    scope.item {
        Manga.Component.MangaCategorySelector(
            selectedCategory = uiState.manga.category,
            allCategories = uiState.allCategories,
            onUpdateMangaCategory = { id -> onAction(MangaAction.UpdateCategory(id)) }
        )
    }

    scope.item {
        Acerola.Component.Divider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).alpha(0.3f))
    }

    // NOTE: Arquivos Locais
    scope.item {
        SectionHeader(stringResource(id = R.string.title_text_archive_configs_in_app))
    }

    scope.item {
        Manga.Component.SyncMangaArchive(
            onSyncChapters = { onSyncAction(MangaSyncAction.SyncChaptersLocal) },
            onRescanCover = { onSyncAction(MangaSyncAction.RescanManga) },
        )
    }

    scope.item {
        Acerola.Component.Divider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).alpha(0.3f))
    }

    // NOTE: Metadados Externos
    scope.item {
        SectionHeader(stringResource(id = R.string.title_sync_external_metadata))
    }

    scope.item {
        Manga.Component.SyncMetadata(
            remoteInfo = uiState.manga.remoteInfo,
            onSyncMangadexInfo = { onSyncAction(MangaSyncAction.SyncMangadexInfo) },
            onSyncMangadexChapters = { onSyncAction(MangaSyncAction.SyncMangadexChapters) },
            onSyncComicInfo = { onSyncAction(MangaSyncAction.SyncComicInfo) },
            onSyncComicInfoChapters = { onSyncAction(MangaSyncAction.SyncComicInfoChapters) },
            onSyncAnilistInfo = { onSyncAction(MangaSyncAction.SyncAnilistInfo) },
        )
    }

    scope.item { Spacer(modifier = Modifier.height(32.dp)) }
}
