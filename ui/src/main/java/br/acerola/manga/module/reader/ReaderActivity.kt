package br.acerola.manga.module.reader

import android.content.Context
import android.os.Build
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.common.activity.BaseActivity
import br.acerola.manga.common.navigation.Destination
import br.acerola.manga.config.preference.ReadingMode
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.module.reader.layout.BottomControls
import br.acerola.manga.module.reader.layout.SettingsSheet
import br.acerola.manga.module.reader.layout.TopBar
import br.acerola.manga.ui.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReaderActivity(
    override val startDestinationRes: Int = Destination.READER.route
) : BaseActivity() {

    object PageExtra {
        const val PAGE = "PAGE"
        const val MANGA_ID = "MANGA_ID"
        const val CHAPTER_ID = "CHAPTER_ID"
        const val INITIAL_PAGE = "INITIAL_PAGE"
    }

    companion object {
        private const val TAG = "ReaderActivity"
    }

    override val applyScaffoldPadding: Boolean = false

    val page: ChapterFileDto? by lazy {
        val safeIntent = intent ?: return@lazy null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            safeIntent.extras?.getParcelable(PageExtra.PAGE, ChapterFileDto::class.java)
        } else {
            @Suppress("DEPRECATION") safeIntent.extras?.getParcelable(PageExtra.PAGE)
        }
    }

    override fun NavGraphBuilder.setupNavGraph(
        context: Context,
        navController: NavHostController
    ) {
        composable(route = context.getString(Destination.READER.route)) {
            val mangaId = intent?.getLongExtra(PageExtra.MANGA_ID, -1L) ?: -1L
            val chapterId = intent?.getLongExtra(PageExtra.CHAPTER_ID, -1L) ?: -1L
            val initialPage = intent?.getIntExtra(PageExtra.INITIAL_PAGE, 0) ?: 0

            AcerolaLogger.d(TAG, "Navigating to ReaderScreen. Manga: $mangaId, Chapter: $chapterId", LogSource.UI)

            ReaderScreen(
                chapter = page,
                chapterId = chapterId,
                mangaId = mangaId,
                initialPage = initialPage,
                onBackClick = { finish() }
            )
        }
    }

    @Composable
    override fun BottomBar(navController: NavHostController) = Unit

    @Composable
    override fun TopBar(navController: NavHostController) = Unit
}
