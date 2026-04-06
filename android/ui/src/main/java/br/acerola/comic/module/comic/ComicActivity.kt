package br.acerola.comic.module.comic
import br.acerola.comic.ui.R

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.comic.common.activity.BaseActivity
import br.acerola.comic.common.navigation.Destination
import br.acerola.comic.dto.ComicDto
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ComicActivity(
    override val startDestinationRes: Int = Destination.COMIC.route
) : BaseActivity() {

    object ChapterExtra {
        const val COMIC = "COMIC"
    }

    val manga: ComicDto? by lazy {
        val safeIntent = intent ?: return@lazy null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            safeIntent.extras?.getParcelable(ChapterExtra.COMIC, ComicDto::class.java)
        } else {
            @Suppress("DEPRECATION")
            safeIntent.extras?.getParcelable(ChapterExtra.COMIC)
        }
    }

    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        composable(route = context.getString(Destination.COMIC.route)) {
            if (manga != null) {
                ComicScreen(
                    manga = manga!!,
                    onBackClick = { finish() }
                )
            } else {
                LaunchedEffect(Unit) {
                    finish()
                }
            }
        }
    }

    @Composable
    override fun TopBar(navController: NavHostController) = Unit
}
