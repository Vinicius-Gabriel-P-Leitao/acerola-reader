package br.acerola.manga.module.reader

import android.content.Context
import android.os.Build
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.common.activity.BaseActivity
import br.acerola.manga.common.navigation.Destination
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.module.reader.layout.ReaderBottomControls
import br.acerola.manga.module.reader.layout.ReaderTopBar
import br.acerola.manga.module.reader.state.ReadingMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReaderActivity(
    override val startDestinationRes: Int = Destination.READER.route
) : BaseActivity() {
    override val applyScaffoldPadding: Boolean = false
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

    @Composable
    override fun BottomBar(navController: NavHostController) {
        val state by viewModel.state.collectAsState()

        AnimatedVisibility(
            visible = state.isUiVisible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }) {
            ReaderBottomControls(
                currentPage = state.currentPage,
                pageCount = state.pageCount,
                onPrevClick = { viewModel.onSliderChanged(index = state.currentPage - 1) },
                onNextClick = { viewModel.onSliderChanged(index = state.currentPage + 1) },
                enableNavigation = state.readingMode != ReadingMode.WEBTOON
            )
        }
    }

    @Composable
    override fun TopBar(navController: NavHostController) {
        val state by viewModel.state.collectAsState()
        var showMenu by remember { mutableStateOf(value = false) }

        Box {
            ReaderTopBar(
                title = page?.name ?: "Leitor",
                subtitle = "Ordem: ${page?.chapterSort ?: "-"}",
                isVisible = state.isUiVisible,
                onBackClick = { finish() },
                onSettingsClick = { showMenu = true })

            if (showMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.TopEnd)
                        .padding(
                            top = 48.dp, end = 8.dp
                        )
                ) {
                    DropdownMenu(
                        expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text(text = "Paginado") }, onClick = {
                            viewModel.updateReadingMode(mode = ReadingMode.HORIZONTAL)
                            showMenu = false
                        })
                        DropdownMenuItem(text = { Text(text = "Vertical") }, onClick = {
                            viewModel.updateReadingMode(mode = ReadingMode.VERTICAL)
                            showMenu = false
                        })
                        DropdownMenuItem(text = { Text(text = "Webtoon") }, onClick = {
                            viewModel.updateReadingMode(mode = ReadingMode.WEBTOON)
                            showMenu = false
                        })
                    }
                }
            }
        }
    }
}
