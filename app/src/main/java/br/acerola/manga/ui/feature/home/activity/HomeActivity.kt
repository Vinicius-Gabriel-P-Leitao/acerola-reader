package br.acerola.manga.ui.feature.home.activity

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.R
import br.acerola.manga.domain.database.AcerolaDatabase
import br.acerola.manga.domain.permission.FolderAccessManager
import br.acerola.manga.ui.common.activity.BaseActivity
import br.acerola.manga.ui.common.component.ButtonType
import br.acerola.manga.ui.common.component.SmartButton
import br.acerola.manga.ui.common.theme.AcerolaTheme
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModelFactory
import br.acerola.manga.ui.feature.config.activity.ConfigActivity
import br.acerola.manga.ui.feature.config.screen.FolderAccessScreen

class HomeActivity(override val startDestination: String = "home") : BaseActivity() {

    private val database by lazy {
        AcerolaDatabase.getInstance(applicationContext)
    }

    override fun NavGraphBuilder.setupNavGraph(navController: NavHostController) {
        composable(route = "home") {
            homeScreen()
        }
    }

    @Composable
    fun homeScreen() {
        AcerolaTheme {
            Scaffold() {
                Column {
                    SmartButton(type = ButtonType.ICON_TEXT, onClick = { println("Clicou!") }, text = "Botão") {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Favorito"
                        )
                    }
                }
            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    override fun navigationBar(navController: NavHostController, extraActions: @Composable RowScope.() -> Unit) {
        super.navigationBar(navController) {
            filterButton()
            settingsNavigation()
        }
    }

    @Composable
    fun filterButton() {
        val context = LocalContext.current

        SmartButton(type = ButtonType.ICON, onClick = {
            println("Filtrar")
        }) {
            Icon(
                imageVector = Icons.Filled.FilterList,
                contentDescription = context.getString(R.string.description_icon_filter_mangas_catalog)
            )
        }
    }
}