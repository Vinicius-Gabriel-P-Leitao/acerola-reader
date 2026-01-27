package br.acerola.manga.module.reader

import android.content.Context
import android.os.Build
import androidx.activity.viewModels
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.common.activity.BaseActivity
import br.acerola.manga.common.navigation.Destination
import br.acerola.manga.dto.archive.ChapterFileDto
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReaderActivity(
    override val startDestinationRes: Int = Destination.READER.route
) : BaseActivity() {

    private val viewModel: ReaderViewModel by viewModels()

    object PageExtra {

        const val PAGE = "PAGE"
    }

    val page: ChapterFileDto? by lazy {
        val safeIntent = intent ?: return@lazy null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            safeIntent.extras?.getParcelable(PageExtra.PAGE, ChapterFileDto::class.java)
        } else {
            @Suppress("DEPRECATION") safeIntent.extras?.getParcelable(PageExtra.PAGE)
        }
    }

    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        composable(route = context.getString(Destination.READER.route)) {
            ReaderScreen(viewModel = viewModel, chapter = page)
        }
    }
}