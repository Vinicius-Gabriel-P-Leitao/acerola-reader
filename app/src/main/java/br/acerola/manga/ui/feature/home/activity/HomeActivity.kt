package br.acerola.manga.ui.feature.home.activity

import android.content.Context
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
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.R
import br.acerola.manga.domain.database.AcerolaDatabase
import br.acerola.manga.shared.route.Destination
import br.acerola.manga.ui.common.activity.BaseActivity
import br.acerola.manga.ui.common.component.ButtonType
import br.acerola.manga.ui.common.component.SmartButton
import br.acerola.manga.ui.common.theme.AcerolaTheme

class HomeActivity(
    override val startDestinationRes: Int = Destination.HOME.route
) : BaseActivity() {

    private val database by lazy {
        AcerolaDatabase.getInstance(applicationContext)
    }

    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        composable(route = context.getString(Destination.HOME.route)) { HomeScreen() }
    }

    @Composable
    fun HomeScreen() {
        AcerolaTheme {
            Scaffold() { _padding ->
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
    override fun TopBar(navController: NavHostController, extraActions: @Composable RowScope.() -> Unit) {
        super.TopBar(navController) {
            FilterButton()
        }
    }

    @Composable
    fun FilterButton() {
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