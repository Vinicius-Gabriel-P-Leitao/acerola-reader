package br.acerola.manga.module.manga

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.common.activity.BaseActivity
import br.acerola.manga.common.navigation.Destination
import br.acerola.manga.dto.MangaDto
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MangaActivity(
    override val startDestinationRes: Int = Destination.MANGA.route
) : BaseActivity() {

    object ChapterExtra {
        const val MANGA = "MANGA"
    }

    val manga: MangaDto? by lazy {
        val safeIntent = intent ?: return@lazy null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            safeIntent.extras?.getParcelable(ChapterExtra.MANGA, MangaDto::class.java)
        } else {
            @Suppress("DEPRECATION")
            safeIntent.extras?.getParcelable(ChapterExtra.MANGA)
        }
    }

    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        composable(route = context.getString(Destination.MANGA.route)) {
            if (manga != null) {
                MangaScreen(
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
