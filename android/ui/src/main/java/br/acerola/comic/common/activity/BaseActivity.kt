package br.acerola.comic.common.activity
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.SnackbarError
import br.acerola.comic.common.ux.component.SnackbarSuccess
import br.acerola.comic.common.ux.component.SnackbarVariant
import br.acerola.comic.common.ux.component.SnackbarWarn
import br.acerola.comic.common.ux.component.resolveSnackbarVariant
import br.acerola.comic.common.ux.layout.ProgressIndicator
import br.acerola.comic.common.ux.layout.Scaffold
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.common.ux.theme.local.LocalSnackbarHostState
import br.acerola.comic.common.viewmodel.progress.GlobalProgressViewModel
import br.acerola.comic.common.viewmodel.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
abstract class BaseActivity : ComponentActivity() {
    abstract val startDestinationRes: Int

    open val applyScaffoldPadding: Boolean = true

    private val themeViewModel: ThemeViewModel by viewModels()
    private val globalProgressViewModel: GlobalProgressViewModel by viewModels()

    open fun NavGraphBuilder.setupNavGraph(
        context: Context,
        navController: NavHostController,
    ) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val currentTheme by themeViewModel.currentTheme.collectAsState()

            AcerolaTheme(theme = currentTheme) {
                val navController = rememberNavController()
                val startDestination = getString(startDestinationRes)
                val snackbarHostState = remember { SnackbarHostState() }
                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                CompositionLocalProvider(
                    value = LocalSnackbarHostState provides snackbarHostState,
                ) {
                    Acerola.Layout.Scaffold {
                        Scaffold(
                            topBar = { TopBar(navController) },
                            snackbarHost = {
                                SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                                    val message = snackbarData.visuals.message
                                    when (resolveSnackbarVariant(snackbarData.visuals)) {
                                        SnackbarVariant.Error -> Acerola.Component.SnackbarError(message)
                                        SnackbarVariant.Success -> Acerola.Component.SnackbarSuccess(message)
                                        SnackbarVariant.Warn -> Acerola.Component.SnackbarWarn(message)
                                    }
                                }
                            },
                            bottomBar = { if (!isLandscape) BottomBar(navController) },
                        ) { padding ->

                            val isIndexing by globalProgressViewModel.isIndexing.collectAsStateWithLifecycle(false)
                            val progress by globalProgressViewModel.progress.collectAsStateWithLifecycle(null)

                            val contentPadding = if (applyScaffoldPadding) padding else PaddingValues(all = 0.dp)
                            Row(modifier = Modifier.padding(paddingValues = contentPadding)) {
                                if (isLandscape) SideBar(navController)
                                Box(modifier = Modifier.weight(1f)) {
                                    NavHost(navController, startDestination) { setupNavGraph(context = this@BaseActivity, navController) }
                                    Acerola.Layout.ProgressIndicator(
                                        modifier =
                                            Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(all = 8.dp),
                                        isLoading = isIndexing,
                                        progress = progress,
                                    )
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

    @Composable
    open fun SideBar(navController: NavHostController) {
    }
}
