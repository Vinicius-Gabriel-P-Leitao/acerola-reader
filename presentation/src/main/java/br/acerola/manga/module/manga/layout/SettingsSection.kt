package br.acerola.manga.module.manga.layout

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.component.CardType
import br.acerola.manga.common.component.SmartCard
import br.acerola.manga.common.viewmodel.library.archive.ChapterArchiveViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.ChapterRemoteInfoViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.module.manga.component.ConfigPreferences
import br.acerola.manga.module.manga.component.SyncMangaArchive
import br.acerola.manga.module.manga.component.SyncMangadexData
import br.acerola.manga.presentation.R

fun LazyListScope.settingsSection(
    directory: MangaDirectoryDto,
    remoteInfo: MangaRemoteInfoDto?,
    mangaDirectoryViewModel: MangaDirectoryViewModel,
    chapterArchiveViewModel: ChapterArchiveViewModel,
    mangaRemoteInfoViewModel: MangaRemoteInfoViewModel,
    chapterRemoteInfoViewModel: ChapterRemoteInfoViewModel,
) {
    item {
        SmartCard(
            type = CardType.CONTENT,
            title = stringResource(id = R.string.title_settings_display_config),
            modifier = Modifier.padding(all = 6.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            ConfigPreferences()
        }
    }

    item { Spacer(modifier = Modifier.height(height = 12.dp)) }

    item {
        SmartCard(
            type = CardType.CONTENT,
            title = stringResource(id = R.string.title_settings_file_config),
            modifier = Modifier.padding(all = 6.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            SyncMangaArchive(
                directory = directory,
                mangaDirectoryViewModel = mangaDirectoryViewModel,
                chapterArchiveViewModel = chapterArchiveViewModel,
            )
        }
    }

    item { Spacer(modifier = Modifier.height(height = 12.dp)) }

    item {
        SmartCard(
            type = CardType.CONTENT,
            title = stringResource(id = R.string.title_config_sync_mangadex),
            modifier = Modifier.padding(all = 6.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            SyncMangadexData(
                remoteInfo = remoteInfo,
                mangaRemoteInfoViewModel = mangaRemoteInfoViewModel,
                chapterRemoteInfoViewModel = chapterRemoteInfoViewModel
            )
        }
    }
}