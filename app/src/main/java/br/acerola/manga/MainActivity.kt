package br.acerola.manga

import android.content.Context
import android.os.Bundle
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.common.activity.BaseActivity
import br.acerola.manga.common.navigation.Destination
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.layout.BottomBar
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.config.Screen
import br.acerola.manga.module.main.history.Screen
import br.acerola.manga.module.main.home.Screen
import br.acerola.manga.module.main.search.Screen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity(
    override val startDestinationRes: Int = Destination.HOME.route
) : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
        }
    }

    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        defaultComposable(context, Destination.HOME) {
            Main.Home.Layout.Screen()
        }
        defaultComposable(context, Destination.HISTORY) {
            Main.History.Layout.Screen()
        }
        defaultComposable(context, Destination.SEARCH) {
            Main.Search.Layout.Screen()
        }
        defaultComposable(context, Destination.CONFIG) {
            Main.Config.Layout.Screen()
        }
    }

    @Composable
    override fun TopBar(navController: NavHostController) {
    }

    @Composable
    override fun BottomBar(navController: NavHostController) {
        Acerola.Layout.BottomBar(navController)
    }

    private fun NavGraphBuilder.defaultComposable(
        context: Context,
        destination: Destination,
        content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit
    ) {
        composable(
            route = context.getString(destination.route), enterTransition = {
                scaleIn(
                    initialScale = 0.8f, animationSpec = tween(durationMillis = 300)
                ) + fadeIn(animationSpec = tween(durationMillis = 300))
            },
            exitTransition = {
                scaleOut(
                    targetScale = 0.8f, animationSpec = tween(durationMillis = 300)
                ) + fadeOut(animationSpec = tween(durationMillis = 300))
            },
            popEnterTransition = {
                scaleIn(
                    initialScale = 1.2f, animationSpec = tween(durationMillis = 300)
                ) + fadeIn(animationSpec = tween(durationMillis = 300))
            },
            popExitTransition = {
                scaleOut(
                    targetScale = 1.2f, animationSpec = tween(durationMillis = 300)
                ) + fadeOut(animationSpec = tween(durationMillis = 300))
            },
            content = content
        )
    }
}
