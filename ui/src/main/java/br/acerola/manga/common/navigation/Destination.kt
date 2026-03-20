package br.acerola.manga.common.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import br.acerola.manga.ui.R

enum class Destination(
    val icon: ImageVector,
    @param:StringRes val label: Int,
    @param:StringRes val route: Int,
    @param:StringRes val contentDescriptionRes: Int,
) {
    HOME(
        icon = Icons.Default.Home,
        label = R.string.label_home_activity,
        route = R.string.navigation_home_activity,
        contentDescriptionRes = R.string.description_home_activity,
    ),
    MANGA(
        icon = Icons.AutoMirrored.Filled.MenuBook,
        label = R.string.label_chapters_activity,
        route = R.string.navigation_chapters_activity,
        contentDescriptionRes = R.string.description_chapters_activity,
    ),
    READER(
        icon = Icons.Default.Book,
        label = R.string.label_reader_activity,
        route = R.string.navigation_reader_activity,
        contentDescriptionRes = R.string.description_reader_activity,
    ),
    HISTORY(
        icon = Icons.Default.History,
        label = R.string.label_history_activity,
        route = R.string.navigation_history_activity,
        contentDescriptionRes = R.string.description_history_activity,
    ),
    SEARCH(
        icon = Icons.Default.Search,
        label = R.string.label_search_activity,
        route = R.string.navigation_search_activity,
        contentDescriptionRes = R.string.description_search_activity,
    ),
    CONFIG(
        icon = Icons.Default.Settings,
        label = R.string.label_config_activity,
        route = R.string.navigation_config_activity,
        contentDescriptionRes = R.string.description_config_activity,
    ),
}