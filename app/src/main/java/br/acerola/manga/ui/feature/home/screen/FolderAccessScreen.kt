package br.acerola.manga.ui.feature.home.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import br.acerola.manga.ui.common.component.ButtonType
import br.acerola.manga.ui.common.component.SmartButton
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel

@Composable
fun FolderAccessScreen(viewModel: FolderAccessViewModel) {
    val folderUri by remember { derivedStateOf { viewModel.folderUri } }
    val scannedFiles by remember { derivedStateOf { viewModel.scannedFiles } }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        viewModel.saveFolderUri(uri)
    }

    LaunchedEffect(key1 = Unit) {
        viewModel.loadSavedFolder()
    }

    Column {
        SmartButton(
            type = ButtonType.TEXT,
            text = "Selecionar pasta",
            onClick = { launcher.launch(input = null) },
        )

        folderUri?.let { uri ->
            Text(text = "Pasta selecionada: $uri")
        }

        scannedFiles.forEach { fileUri ->
            Text(text = fileUri.lastPathSegment ?: "Arquivo")
        }
    }
}