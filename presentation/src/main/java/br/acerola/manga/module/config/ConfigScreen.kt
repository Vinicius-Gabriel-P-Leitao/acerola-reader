package br.acerola.manga.module.config

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.manga.presentation.R
import br.acerola.manga.common.component.CardType
import br.acerola.manga.common.component.SmartCard
import br.acerola.manga.common.viewmodel.archive.FilePreferencesViewModel
import br.acerola.manga.common.viewmodel.archive.FileSystemAccessViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.module.config.component.SelectFolder
import br.acerola.manga.module.config.component.PreferSavedFile
import br.acerola.manga.module.config.component.SyncLibraryArchive
import br.acerola.manga.module.config.component.SyncMangadexData

@Composable
fun ConfigScreen(
    filePreferencesViewModel: FilePreferencesViewModel,
    fileSystemAccessViewModel: FileSystemAccessViewModel,
    mangaDirectoryViewModel: MangaDirectoryViewModel,
    mangaDexViewModel: MangaRemoteInfoViewModel
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(modifier = Modifier.padding(all = 6.dp)) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            SmartCard(
                type = CardType.CONTENT,
                title = stringResource(id = R.string.title_text_archive_configs_in_app),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            ) {
                SelectFolder(context, fileSystemAccessViewModel)

                Spacer(modifier = Modifier.height(height = 12.dp))

                PreferSavedFile(filePreferencesViewModel)

                Spacer(modifier = Modifier.height(height = 12.dp))

                SyncLibraryArchive(mangaDirectoryViewModel)
            }

            Spacer(modifier = Modifier.height(height = 12.dp))

            SmartCard(
                type = CardType.CONTENT,
                title = stringResource(id = R.string.title_text_mangadex_configs_in_app),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            ) {
                SyncMangadexData(mangaDexViewModel)
            }

            Spacer(modifier = Modifier.height(height = 16.dp))
        }
    }
}

