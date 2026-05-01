package br.acerola.comic.module.reader
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.comic.common.activity.BaseActivity
import br.acerola.comic.common.navigation.Destination
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReaderActivity(
    override val startDestinationRes: Int = Destination.READER.route,
) : BaseActivity() {
    object PageExtra {
        const val PAGE = "PAGE"
        const val MANGA_ID = "MANGA_ID"
        const val CHAPTER_ID = "CHAPTER_ID"
        const val CHAPTER_SORT = "CHAPTER_SORT"
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
            @Suppress("DEPRECATION")
            safeIntent.extras?.getParcelable(PageExtra.PAGE)
        }
    }

    override fun NavGraphBuilder.setupNavGraph(
        context: Context,
        navController: NavHostController,
    ) {
        composable(route = context.getString(Destination.READER.route)) {
            val comicId = intent?.getLongExtra(PageExtra.MANGA_ID, -1L) ?: -1L
            val chapterId = intent?.getLongExtra(PageExtra.CHAPTER_ID, -1L) ?: -1L
            val chapterSort = intent?.getStringExtra(PageExtra.CHAPTER_SORT) ?: ""
            val initialPage = intent?.getIntExtra(PageExtra.INITIAL_PAGE, 0) ?: 0

            AcerolaLogger.d(TAG, "Navigating to ReaderScreen. Comic: $comicId, ChapterId: $chapterId, ChapterSort: $chapterSort", LogSource.UI)

            ReaderScreen(
                chapter = page,
                chapterId = chapterId,
                chapterSort = chapterSort,
                comicId = comicId,
                initialPage = initialPage,
                onBackClick = { finish() },
            )
        }
    }

    @Composable
    override fun BottomBar(navController: NavHostController) = Unit

    @Composable
    override fun TopBar(navController: NavHostController) = Unit
}
