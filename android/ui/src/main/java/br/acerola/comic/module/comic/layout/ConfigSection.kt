package br.acerola.comic.module.comic.layout

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.comic.module.comic.Comic
import br.acerola.comic.module.comic.component.ComicCategorySelector
import br.acerola.comic.module.comic.component.ComicExternalSyncToggle
import br.acerola.comic.module.comic.component.PaginationPreference
import br.acerola.comic.module.comic.component.SyncMangaArchive
import br.acerola.comic.module.comic.component.SyncMetadata
import br.acerola.comic.module.comic.state.ComicAction
import br.acerola.comic.module.comic.state.ComicSyncAction
import br.acerola.comic.module.comic.state.ComicUiState
import br.acerola.comic.ui.R

private val itemModifier = Modifier.padding(horizontal = 16.dp)

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp),
    )
}

fun Comic.Layout.configSection(
    scope: LazyListScope,
    uiState: ComicUiState,
    onAction: (ComicAction) -> Unit,
    onSyncAction: (ComicSyncAction) -> Unit,
) {
    scope.item { Spacer(modifier = Modifier.height(16.dp)) }

    // NOTE: Configurações de Exibição
    scope.item {
        SectionHeader(stringResource(id = R.string.title_settings_display_config))
    }

    scope.item {
        Comic.Component.PaginationPreference(
            selected = uiState.selectedChapterPerPage,
            onSelect = { onAction(ComicAction.UpdatePageSize(it)) },
            modifier = itemModifier,
        )
    }

    scope.item {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).alpha(0.3f))
    }

    // NOTE: Categorias
    scope.item {
        SectionHeader(stringResource(id = R.string.title_config_categories))
    }

    scope.item {
        Comic.Component.ComicCategorySelector(
            selectedCategory = uiState.comic.category,
            allCategories = uiState.allCategories,
            onUpdateMangaCategory = { id -> onAction(ComicAction.UpdateCategory(id)) },
            modifier = itemModifier,
        )
    }

    scope.item {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).alpha(0.3f))
    }

    // NOTE: Arquivos Locais
    scope.item {
        SectionHeader(stringResource(id = R.string.title_text_archive_configs_in_app))
    }

    scope.item {
        Comic.Component.SyncMangaArchive(
            onSyncChapters = { onSyncAction(ComicSyncAction.SyncChaptersLocal) },
            onRescanCover = { onSyncAction(ComicSyncAction.RescanComic) },
            onExtractFirstPageAsCover = { onSyncAction(ComicSyncAction.ExtractFirstPageAsCover) },
            modifier = itemModifier,
        )
    }

    scope.item {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).alpha(0.3f))
    }

    // NOTE: Metadados Externos
    scope.item {
        SectionHeader(stringResource(id = R.string.title_sync_external_metadata))
    }

    scope.item {
        Comic.Component.ComicExternalSyncToggle(
            enabled = uiState.comic.directory.externalSyncEnabled,
            onToggle = { onAction(ComicAction.ToggleExternalSync(it)) },
            modifier = itemModifier,
        )
    }

    scope.item { Spacer(modifier = Modifier.height(8.dp)) }

    scope.item {
        Comic.Component.SyncMetadata(
            remoteInfo = uiState.comic.remoteInfo,
            externalSyncEnabled = uiState.comic.directory.externalSyncEnabled,
            onSyncMangadexInfo = { onSyncAction(ComicSyncAction.SyncMangadexInfo) },
            onSyncMangadexChapters = { onSyncAction(ComicSyncAction.SyncMangadexChapters) },
            onSyncComicInfo = { onSyncAction(ComicSyncAction.SyncComicInfo) },
            onSyncComicInfoChapters = { onSyncAction(ComicSyncAction.SyncComicInfoChapters) },
            onSyncAnilistInfo = { onSyncAction(ComicSyncAction.SyncAnilistInfo) },
            modifier = itemModifier,
        )
    }

    scope.item { Spacer(modifier = Modifier.height(32.dp)) }
}
