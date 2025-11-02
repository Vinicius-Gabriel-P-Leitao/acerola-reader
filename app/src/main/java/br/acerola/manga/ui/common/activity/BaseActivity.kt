package br.acerola.manga.ui.common.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import br.acerola.manga.R
import br.acerola.manga.ui.common.component.ButtonType
import br.acerola.manga.ui.common.component.SmartButton
import br.acerola.manga.ui.common.layout.TopBar
import br.acerola.manga.ui.common.layout.AcerolaScaffold
import br.acerola.manga.ui.common.theme.AcerolaTheme
import br.acerola.manga.ui.feature.config.activity.ConfigActivity

abstract class BaseActivity : ComponentActivity() {
    abstract val startDestination: String

    open fun NavGraphBuilder.setupNavGraph(navController: NavHostController) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AcerolaTheme() {
                val navController = rememberNavController()

                AcerolaScaffold {
                    Scaffold(
                        topBar = { navigationBar(navController) }
                    ) { padding ->
                        Box(modifier = Modifier.padding(paddingValues = padding)) {
                            NavHost(
                                navController = navController,
                                startDestination = startDestination
                            ) {
                                setupNavGraph(navController)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    open fun navigationBar(navController: NavHostController, extraActions: @Composable RowScope.() -> Unit = {}) {
        TopBar(navController, extraActions)
    }

    @Composable
    fun settingsNavigation() {
        val context = LocalContext.current

        SmartButton(type = ButtonType.ICON, onClick = {
            val intent = Intent(context, ConfigActivity::class.java)
            context.startActivity(intent)
        }) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = context.getString(R.string.description_icon_settings_activity)
            )
        }
    }
}
