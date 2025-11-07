package br.acerola.manga.ui.feature.main.config.layout

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import br.acerola.manga.R
import br.acerola.manga.ui.common.component.ButtonType
import br.acerola.manga.ui.common.component.SmartButton
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel

@Composable
fun FolderAccess(context: Context, viewModel: FolderAccessViewModel, onFolderSelected: (String) -> Unit = {}) {
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
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = context.getString(R.string.description_icon_select_folder_mangas),
                    modifier = Modifier
                        .size(size = 40.dp)
                        .padding(all = 4.dp),
                )
            }
        }
    }
}