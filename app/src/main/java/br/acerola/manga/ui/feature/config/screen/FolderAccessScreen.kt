package br.acerola.manga.ui.feature.config.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
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
            type = ButtonType.ICON,
            onClick = { launcher.launch(input = null) },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size = 34.dp)
                    .clip(CircleShape)
                    .background(color = MaterialTheme.colorScheme.primary),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Adicionar pasta",
                    modifier = Modifier
                        .size(size = 40.dp)
                        .padding(all = 4.dp),
                )
            }
        }

        folderUri?.let { uri ->
            Text(text = "Pasta selecionada: $uri")
        }

//        scannedFiles.forEach { fileUri ->
//            Text(text = fileUri.lastPathSegment ?: "Arquivo")
//        }
    }
}