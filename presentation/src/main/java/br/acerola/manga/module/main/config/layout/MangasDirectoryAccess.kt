package br.acerola.manga.module.main.config.layout

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.IconButton
import br.acerola.manga.common.viewmodel.archive.FileSystemAccessViewModel
import br.acerola.manga.presentation.R

@Composable
fun FolderAccess(viewModel: FileSystemAccessViewModel, onFolderSelected: (String) -> Unit = {}) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            viewModel.saveFolderUri(uri)
            uri?.let { onFolderSelected(it.toString()) }
        }
    )

    LaunchedEffect(key1 = Unit) {
        viewModel.loadSavedFolder()
        viewModel.folderUri?.let { onFolderSelected(it.toString()) }
    }

    Column {
        Acerola.Component.IconButton(
            onClick = { launcher.launch(input = null) },
            icon = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(size = 34.dp)
                        .clip(CircleShape)
                        .background(color = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.description_icon_select_folder_mangas),
                        modifier = Modifier
                            .size(size = 40.dp)
                            .padding(all = 4.dp),
                    )
                }
            }
        )
    }
}
