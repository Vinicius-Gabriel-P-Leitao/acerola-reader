package br.acerola.manga.ui.feature.main.activity

import android.content.Context
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.shared.route.Destination
import br.acerola.manga.ui.common.activity.BaseActivity
import br.acerola.manga.ui.common.layout.NavigationBottomBar
import br.acerola.manga.ui.common.layout.NavigationTopBar
import br.acerola.manga.ui.feature.main.config.screen.ConfigScreen
import br.acerola.manga.ui.feature.main.history.screen.HistoryScreen
import br.acerola.manga.ui.feature.main.home.screen.HomeScreen

class MainActivity : BaseActivity() {
    override val startDestinationRes: Int = Destination.HOME.route
    private val topBarActions = mutableStateOf<@Composable RowScope.() -> Unit>(value = {})

    val defaultEnterTransition: EnterTransition =
        scaleIn(initialScale = 0.8f, animationSpec = tween(300)) +
                fadeIn(animationSpec = tween(300))

    val defaultExitTransition: ExitTransition =
        scaleOut(targetScale = 0.8f, animationSpec = tween(300)) +
                fadeOut(animationSpec = tween(300))

    val defaultPopEnterTransition: EnterTransition =
        scaleIn(initialScale = 1.2f, animationSpec = tween(300)) +
                fadeIn(animationSpec = tween(300))

    val defaultPopExitTransition: ExitTransition =
        scaleOut(targetScale = 1.2f, animationSpec = tween(300)) +
                fadeOut(animationSpec = tween(300))

    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        composable(
            route = context.getString(Destination.HOME.route),
            enterTransition = { defaultEnterTransition },
            exitTransition = { defaultExitTransition },
            popEnterTransition = { defaultPopEnterTransition },
            popExitTransition = { defaultPopExitTransition },
        ) {
            HomeScreen(onSetTopBarActions = { actions ->
                topBarActions.value = actions
            })
        }

        composable(
            route = context.getString(Destination.HISTORY.route),
            enterTransition = { defaultEnterTransition },
            exitTransition = { defaultExitTransition },
            popEnterTransition = { defaultPopEnterTransition },
            popExitTransition = { defaultPopExitTransition },
        ) {
            HistoryScreen()
        }

        composable(
            route = context.getString(Destination.CONFIG.route),
            enterTransition = { defaultEnterTransition },
            exitTransition = { defaultExitTransition },
            popEnterTransition = { defaultPopEnterTransition },
            popExitTransition = { defaultPopExitTransition },
        ) {
            ConfigScreen()
        }
    }

    @Composable
    override fun TopBar(navController: NavHostController) {
        NavigationTopBar(navController, extraActions = topBarActions.value)
    }

    @Composable
    override fun BottomBar(navController: NavHostController) {
        NavigationBottomBar(navController)
    }

}