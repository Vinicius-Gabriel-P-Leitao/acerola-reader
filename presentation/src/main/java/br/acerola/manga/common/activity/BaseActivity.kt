package br.acerola.manga.common.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import br.acerola.manga.common.layout.AcerolaScaffold
import br.acerola.manga.common.layout.LocalSnackbarHostState
import br.acerola.manga.common.theme.AcerolaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
abstract class BaseActivity : ComponentActivity() {
    abstract val startDestinationRes: Int

    open fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            AcerolaTheme() {
                val navController = rememberNavController()
                val startDestination = getString(startDestinationRes)
                val snackbarHostState = remember { SnackbarHostState() }

                CompositionLocalProvider(value = LocalSnackbarHostState provides snackbarHostState) {
                    AcerolaScaffold {
                        Scaffold(
                            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                            topBar = { TopBar(navController) },
                            bottomBar = { BottomBar(navController) }) { padding ->
                            Box(modifier = Modifier.padding(paddingValues = padding)) {
                                NavHost(
                                    navController, startDestination
                                ) {
                                    setupNavGraph(context = this@BaseActivity, navController)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    open fun TopBar(navController: NavHostController) {
    }

    @Composable
    open fun BottomBar(navController: NavHostController) {
    }
}