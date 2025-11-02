package br.acerola.manga.shared.route

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import br.acerola.manga.R
import br.acerola.manga.ui.feature.config.activity.ConfigActivity
import br.acerola.manga.ui.feature.history.activity.HistoryActivity
import br.acerola.manga.ui.feature.home.activity.HomeActivity
import kotlin.reflect.KClass

enum class Destination(
    val icon: ImageVector,
    @param:StringRes val label: Int,
    @param:StringRes val route: Int,
    @param:StringRes val contentDescriptionRes: Int,
    val activityClass: KClass<out ComponentActivity>? = null
) {
    HOME(
        icon = Icons.Default.Home,
        label = R.string.label_home_activity,
        route = R.string.navigation_home_activity,
        contentDescriptionRes = R.string.description_home_activity,
        activityClass = HomeActivity::class,
    ),
    HISTORY(
        icon = Icons.Default.History,
        label = R.string.label_history_activity,
        route = R.string.navigation_history_activity,
        contentDescriptionRes = R.string.description_history_activity,
        activityClass = HistoryActivity::class
    ),
    CONFIG(
        icon = Icons.Default.Settings,
        label = R.string.label_config_activity,
        route = R.string.navigation_config_activity,
        contentDescriptionRes = R.string.description_config_activity,
        activityClass = ConfigActivity::class,
    ),
}