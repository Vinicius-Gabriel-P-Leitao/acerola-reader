package br.acerola.manga.ui.feature.history.activity

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.shared.route.Destination
import br.acerola.manga.ui.common.activity.BaseActivity
import br.acerola.manga.ui.common.layout.NavigationBottomBar
import br.acerola.manga.ui.common.layout.NavigationTopBar

class HistoryActivity(
    override val startDestinationRes: Int = Destination.HISTORY.route
) : BaseActivity() {
    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        composable(route = context.getString(Destination.HISTORY.route)) { HistoryScreen() }
    }

    @Composable
    override fun TopBar(navController: NavHostController) {
        NavigationTopBar(navController)
    }

    @Composable
    override fun BottomBar(navController: NavHostController) {
        NavigationBottomBar(navController)
    }

    @Composable
    fun HistoryScreen() {
        Text(text = "Tela de histórico")
    }
}