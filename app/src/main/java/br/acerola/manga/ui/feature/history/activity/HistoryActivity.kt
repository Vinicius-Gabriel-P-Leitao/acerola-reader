package br.acerola.manga.ui.feature.history.activity

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.shared.route.Destination
import br.acerola.manga.ui.common.activity.BaseActivity

class HistoryActivity(
    override val startDestinationRes: Int = Destination.HISTORY.route
) : BaseActivity() {
    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        composable(route = context.getString(Destination.HISTORY.route)) { HistoryScreen() }
    }

    @Composable
    fun HistoryScreen() {
        Text(text = "Tela de histórico")
    }
}