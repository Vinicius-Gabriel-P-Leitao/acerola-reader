package br.acerola.manga.module.manga

import android.content.Context
import android.os.Build
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.common.activity.BaseActivity
import br.acerola.manga.common.layout.NavigationTopBar
import br.acerola.manga.common.navigation.Destination
import br.acerola.manga.common.viewmodel.library.archive.ChapterArchiveViewModel
import br.acerola.manga.dto.MangaDto
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MangaActivity(
    override val startDestinationRes: Int = Destination.CHAPTERS.route
) : BaseActivity() {
    private val chapterViewModel: ChapterArchiveViewModel by viewModels()

    object ChapterExtra {
        const val MANGA = "MANGA"
    }

    val manga: MangaDto? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(ChapterExtra.MANGA, MangaDto::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra(ChapterExtra.MANGA)
        }
    }

    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        composable(route = context.getString(Destination.CHAPTERS.route)) {
            manga?.let {
                Screen(chapterViewModel, manga = it)
            }
        }
    }

    @Composable
    override fun TopBar(navController: NavHostController) {
        NavigationTopBar(navController)
    }
}