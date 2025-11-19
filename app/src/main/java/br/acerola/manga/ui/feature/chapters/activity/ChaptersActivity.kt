package br.acerola.manga.ui.feature.chapters.activity

import android.content.Context
import android.os.Build
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.R
import br.acerola.manga.domain.database.AcerolaDatabase
import br.acerola.manga.domain.service.library.chapter.ChapterFileService
import br.acerola.manga.shared.dto.archive.MangaFolderDto
import br.acerola.manga.shared.route.Destination
import br.acerola.manga.ui.common.activity.BaseActivity
import br.acerola.manga.ui.common.component.CardType
import br.acerola.manga.ui.common.component.SmartCard
import br.acerola.manga.ui.common.layout.NavigationTopBar
import br.acerola.manga.ui.common.viewmodel.library.ChapterViewModel
import br.acerola.manga.ui.common.viewmodel.library.ChapterViewModelFactory
import br.acerola.manga.ui.feature.chapters.component.ChapterItem
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import coil.size.SizeResolver

class ChaptersActivity(
    override val startDestinationRes: Int = Destination.CHAPTERS.route
) : BaseActivity() {
    private val chapterViewModel: ChapterViewModel by viewModels {
        val database = AcerolaDatabase.getInstance(context = this)
        ChapterViewModelFactory(
            application, chapterOperations = ChapterFileService(
                chapterDao = database.chapterFileDao()
            )
        )
    }

    val folder: MangaFolderDto? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("folder", MangaFolderDto::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra("folder")
        }
    }

    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        composable(route = context.getString(Destination.CHAPTERS.route)) { backStackEntry ->
            folder?.let {
                Screen(chapterViewModel, folder = it)
            } ?: run {

            }
        }
    }

    @Composable
    override fun TopBar(navController: NavHostController) {
        NavigationTopBar(navController)
    }

    @Composable
    fun Screen(chapterViewModel: ChapterViewModel, folder: MangaFolderDto) {
        val context = LocalContext.current
        val density = LocalDensity.current

        LaunchedEffect(key1 = folder.id) {
            chapterViewModel.init(folderId = folder.id, firstPage = folder.chapters)
        }

        val chapterPage by chapterViewModel.chapterPage.collectAsState()
        val chapters = chapterPage?.items ?: emptyList()
        val total = chapterPage?.total ?: 0

        val imageRequest = remember(key1 = folder.coverUri) {
            val imageSize: Size = with(receiver = density) {
                Size(width = 120.dp.toPx().toInt(), height = 180.dp.toPx().toInt())
            }
            ImageRequest.Builder(context).data(folder.coverUri).size(resolver = SizeResolver(imageSize)).build()
        }

        val coverPainter = rememberAsyncImagePainter(
            model = imageRequest,
            placeholder = painterResource(id = R.drawable.ic_launcher_background),
            error = painterResource(id = R.drawable.ic_launcher_background)
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            SmartCard(
                type = CardType.IMAGE,
                image = coverPainter,
                modifier = Modifier
                    .width(width = 120.dp)
                    .height(height = 180.dp)
                    .padding(vertical = 4.dp, horizontal = 8.dp),
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f)
            ) {
                items(
                    items = chapters, key = { it.id }) { chapter ->
                    ChapterItem(chapter = chapter, onClick = { /* abrir leitor */ })
                    Spacer(modifier = Modifier.height(height = 6.dp))
                }

                // TODO: Botar um botão de paginação, tem que otimizar essa porqueira.
                if (chapters.size < total) {
                    item {
                        LaunchedEffect(Unit) {
                            chapterViewModel.loadNextPage()
                        }
                        Text(
                            text = "Carregando mais capítulos...", modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}