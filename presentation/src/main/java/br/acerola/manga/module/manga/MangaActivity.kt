package br.acerola.manga.module.manga

import android.content.Context
import android.os.Build
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.common.activity.BaseActivity
import br.acerola.manga.common.layout.LocalSnackbarHostState
import br.acerola.manga.common.layout.NavigationTopBar
import br.acerola.manga.common.navigation.Destination
import br.acerola.manga.common.viewmodel.library.archive.ChapterArchiveViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.common.viewmodel.library.metadata.ChapterRemoteInfoViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.dto.MangaDto
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MangaActivity(
    override val startDestinationRes: Int = Destination.CHAPTERS.route
) : BaseActivity() {

    private val chapterRemoteInfoViewModel: ChapterRemoteInfoViewModel by viewModels()
    private val mangaRemoteInfoViewModel: MangaRemoteInfoViewModel by viewModels()
    private val chapterArchiveViewModel: ChapterArchiveViewModel by viewModels()
    private val mangaDirectoryViewModel: MangaDirectoryViewModel by viewModels()
    private val mangaViewModel: MangaViewModel by viewModels()

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
        composable(route = context.getString(Destination.CHAPTERS.route)) {
            if (manga != null) {
                MangaScreen(
                    manga = manga!!,
                    mangaViewModel = mangaViewModel,
                    mangaDirectoryViewModel = mangaDirectoryViewModel,
                    chapterArchiveViewModel = chapterArchiveViewModel,
                    mangaRemoteInfoViewModel = mangaRemoteInfoViewModel,
                    chapterRemoteInfoViewModel = chapterRemoteInfoViewModel
                )
            } else {
                LaunchedEffect(Unit) {
                    finish()
                }
            }
        }
    }

    @Composable
    override fun TopBar(navController: NavHostController) {
        val snackbarHostState = LocalSnackbarHostState.current
        val context = LocalContext.current

        // TODO: Ver por que isso tá aqui
        LaunchedEffect(key1 = Unit) {
            mangaViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context))
            }
        }

        LaunchedEffect(key1 = Unit) {
            mangaDirectoryViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context))
            }
        }

        LaunchedEffect(key1 = Unit) {
            mangaRemoteInfoViewModel.uiEvents.collect { message ->
                snackbarHostState.showSnackbar(message.uiMessage.asString(context))
            }
        }

        NavigationTopBar(navController)
    }
}