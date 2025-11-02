package br.acerola.manga.ui.feature.config.activity

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.R
import br.acerola.manga.domain.permission.FolderAccessManager
import br.acerola.manga.shared.route.Destination
import br.acerola.manga.ui.common.activity.BaseActivity
import br.acerola.manga.ui.common.component.CardType
import br.acerola.manga.ui.common.component.SmartCard
import br.acerola.manga.ui.common.theme.AcerolaTheme
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModelFactory
import br.acerola.manga.ui.feature.config.screen.FolderAccessScreen

class ConfigActivity(
    override val startDestinationRes: Int = Destination.CONFIG.route
) : BaseActivity() {
    private val folderAccessViewModel: FolderAccessViewModel by lazy {
        ViewModelProvider(
            owner = this, factory = FolderAccessViewModelFactory(
                application, manager = FolderAccessManager(applicationContext)
            )
        )[FolderAccessViewModel::class.java]
    }

    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        composable(route = context.getString(Destination.CONFIG.route)) { ConfigScreen() }
    }

    @Composable
    fun ConfigScreen() {
        val context = LocalContext.current

        AcerolaTheme {
            Scaffold(modifier = Modifier.padding(all = 6.dp)) { _padding ->
                Column {
                    SmartCard(
                        type = CardType.CONTENT,
                        title = context.getString(R.string.description_title_text_archive_configs_in_app),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                    ) {
                        SelectFolderCard(context)
                    }

                    Spacer(modifier = Modifier.height(height = 12.dp))

                    SmartCard(
                        type = CardType.CONTENT,
                        title = context.getString(R.string.description_title_text_mangadex_configs_in_app),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                    ) {

                    }
                }
            }
        }
    }

    @Composable
    fun SelectFolderCard(context: Context) {
        var selectedFolderUri by remember { mutableStateOf<String?>(null) }

        SmartCard(
            type = CardType.CONTENT, colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ), elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 8.dp, pressedElevation = 12.dp
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(size = 34.dp)
                            .clip(CircleShape)
                            .background(color = MaterialTheme.colorScheme.primary),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = "Pasta",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .size(size = 40.dp)
                                .padding(all = 4.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(width = 12.dp))

                    Column {
                        Text(
                            text = context.getString(R.string.description_title_text_config_select_path_manga),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = context.getString(R.string.description_text_config_select_path_manga),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(width = 12.dp))

                FolderAccessScreen(context = context, viewModel = folderAccessViewModel) { uri ->
                    selectedFolderUri = uri
                }
            }

            // TODO: Fazer uma vizualização melhor disso, e fazer ele pegar também dá que está no datastore.
            Spacer(modifier = Modifier.height(height = 12.dp))

            selectedFolderUri?.let { uri ->
                Text(
                    text = "Pasta selecionada: $uri", color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}