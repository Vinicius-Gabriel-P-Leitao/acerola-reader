package br.acerola.manga.module.config.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.component.CardType
import br.acerola.manga.common.component.SmartCard
import br.acerola.manga.common.viewmodel.metadata.MetadataSettingsViewModel
import br.acerola.manga.presentation.R

@Composable
fun MetadataExportSettings(
    viewModel: MetadataSettingsViewModel
) {
    val generateComicInfo by viewModel.generateComicInfo.collectAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color = MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = Icons.Filled.Description,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp),
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Gerar ComicInfo.xml",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                text = "Criar arquivo de metadados na pasta ao sincronizar com fontes externas.",
            )
        }

        Switch(
            checked = generateComicInfo,
            onCheckedChange = { viewModel.setGenerateComicInfo(it) }
        )
    }
}
