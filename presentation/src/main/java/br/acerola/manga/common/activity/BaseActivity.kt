package br.acerola.manga.common.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.manga.common.ux.layout.Scaffold
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.common.viewmodel.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
abstract class BaseActivity : ComponentActivity() {
    abstract val startDestinationRes: Int

    open val applyScaffoldPadding: Boolean = true

    private val themeViewModel: ThemeViewModel by viewModels()

    open fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val useDynamicColor by themeViewModel.useDynamicColor.collectAsState()

            AcerolaTheme(dynamicColor = useDynamicColor) {
                val navController = rememberNavController()
                val startDestination = getString(startDestinationRes)
                val snackbarHostState = remember { SnackbarHostState() }

                CompositionLocalProvider(
                    value = LocalSnackbarHostState provides snackbarHostState
                ) {
                    Acerola.Layout.Scaffold {
                        Scaffold(
                            topBar = { TopBar(navController) },
                            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                            bottomBar = { BottomBar(navController) }) { padding ->

                            val contentPadding = if (applyScaffoldPadding) padding else PaddingValues(all = 0.dp)
                            Box(modifier = Modifier.padding(paddingValues = contentPadding)) {
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
