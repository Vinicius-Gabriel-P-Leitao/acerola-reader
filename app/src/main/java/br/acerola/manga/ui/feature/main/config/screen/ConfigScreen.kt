package br.acerola.manga.ui.feature.main.config.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.manga.R
import br.acerola.manga.ui.common.component.CardType
import br.acerola.manga.ui.common.component.SmartCard
import br.acerola.manga.ui.common.viewmodel.archive.file.FilePreferencesViewModel
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel
import br.acerola.manga.ui.common.viewmodel.library.archive.MangaFolderViewModel
import br.acerola.manga.ui.common.viewmodel.library.metadata.MangaMetadataViewModel
import br.acerola.manga.ui.feature.main.config.component.SelectFolder
import br.acerola.manga.ui.feature.main.config.component.SelectedPreferSavedFile
import br.acerola.manga.ui.feature.main.config.component.SyncLibrary

@Composable
fun ConfigScreen(
    folderAccessViewModel: FolderAccessViewModel,
    filePreferencesViewModel: FilePreferencesViewModel,
    mangaFolderViewModel: MangaFolderViewModel,
    mangaMetadataViewModel: MangaMetadataViewModel
) {
    val context = LocalContext.current

    Scaffold(modifier = Modifier.padding(all = 6.dp)) { _ ->
        Column {
            SmartCard(
                type = CardType.CONTENT,
                title = stringResource(id = R.string.title_text_archive_configs_in_app),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            ) {
                SelectFolder(context, folderAccessViewModel)

                Spacer(modifier = Modifier.height(height = 12.dp))

                SelectedPreferSavedFile(filePreferencesViewModel)

                Spacer(modifier = Modifier.height(height = 12.dp))

                SyncLibrary(mangaFolderViewModel, mangaMetadataViewModel)
            }

            Spacer(modifier = Modifier.height(height = 12.dp))

            SmartCard(
                type = CardType.CONTENT,
                title = stringResource(id = R.string.title_text_mangadex_configs_in_app),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            ) {
                // TODO: Conteúdo das configs do MangaDex aqui
            }
        }
    }
}

