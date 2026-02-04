package br.acerola.manga.module.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.acerola.manga.common.component.CardType
import br.acerola.manga.common.component.Divider
import br.acerola.manga.common.component.SmartCard
import br.acerola.manga.common.viewmodel.archive.FilePreferencesViewModel
import br.acerola.manga.common.viewmodel.archive.FileSystemAccessViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.module.config.component.PreferSavedFile
import br.acerola.manga.module.config.component.SelectFolder
import br.acerola.manga.module.config.component.SyncLibraryArchive
import br.acerola.manga.module.config.component.SyncMangadexData
import br.acerola.manga.presentation.R

@Composable
fun ConfigScreen(
    filePreferencesViewModel: FilePreferencesViewModel,
    fileSystemAccessViewModel: FileSystemAccessViewModel,
    mangaDirectoryViewModel: MangaDirectoryViewModel,
    mangaDexViewModel: MangaRemoteInfoViewModel
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Principal da Página
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                // Pequeno detalhe visual no título
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(32.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(id = R.string.label_config_activity),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Seção: Arquivos Locais
            // Usamos a cor Secondary (Pink no seu tema) para destacar arquivos
            SettingsSection(
                title = stringResource(id = R.string.title_text_archive_configs_in_app),
                icon = Icons.Rounded.FolderOpen,
                iconColor = MaterialTheme.colorScheme.secondary
            ) {
                SelectFolder(context, fileSystemAccessViewModel)
                Divider(modifier = Modifier.alpha(0.5f)) // Divider mais sutil
                PreferSavedFile(filePreferencesViewModel)
                Divider(modifier = Modifier.alpha(0.5f))
                SyncLibraryArchive(mangaDirectoryViewModel)
            }

            // Seção: Integração Remota
            // Usamos a cor Tertiary (Sky no seu tema) para nuvem/rede
            SettingsSection(
                title = stringResource(id = R.string.title_text_mangadex_configs_in_app),
                icon = Icons.Rounded.CloudSync,
                iconColor = MaterialTheme.colorScheme.tertiary
            ) {
                SyncMangadexData(mangaDexViewModel)
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// Reutilização do componente privado para consistência
@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    SmartCard(
        type = CardType.CONTENT,
        title = null,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                // Ícone com Container Colorido
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}