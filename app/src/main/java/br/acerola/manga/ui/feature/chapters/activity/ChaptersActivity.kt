package br.acerola.manga.ui.feature.chapters.activity

import android.content.Context
import android.os.Build
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.R
import br.acerola.manga.domain.database.AcerolaDatabase
import br.acerola.manga.domain.service.library.archive.ArchiveMangaService
import br.acerola.manga.shared.dto.archive.MangaFolderDto
import br.acerola.manga.shared.permission.FolderAccessManager
import br.acerola.manga.shared.route.Destination
import br.acerola.manga.ui.common.activity.BaseActivity
import br.acerola.manga.ui.common.component.CardType
import br.acerola.manga.ui.common.component.SmartCard
import br.acerola.manga.ui.common.layout.NavigationTopBar
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModelFactory
import br.acerola.manga.ui.common.viewmodel.library.MangaLibraryViewModel
import br.acerola.manga.ui.common.viewmodel.library.MangaLibraryViewModelFactory
import br.acerola.manga.ui.feature.chapters.component.ChapterItem
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import coil.size.SizeResolver

class ChaptersActivity(
    override val startDestinationRes: Int = Destination.CHAPTERS.route
) : BaseActivity() {
    private val database by lazy {
        AcerolaDatabase.getInstance(applicationContext)
    }

    private val folderAccessViewModel by lazy {
        ViewModelProvider(
            owner = this, factory = FolderAccessViewModelFactory(
                application, manager = FolderAccessManager(applicationContext)
            )
        )[FolderAccessViewModel::class.java]
    }

    private val mangaLibraryViewModel by lazy {
        ViewModelProvider(
            owner = this, factory = MangaLibraryViewModelFactory(
                application, folderAccessViewModel = folderAccessViewModel, libraryPort = ArchiveMangaService(
                    context = applicationContext,
                    folderDao = database.mangaFolderDao(),
                    chapterDao = database.chapterFileDao()
                )
            )
        )[MangaLibraryViewModel::class.java]
    }

    val folder: MangaFolderDto? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("folder", MangaFolderDto::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra<MangaFolderDto>("folder")
        }
    }

    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        composable(route = context.getString(Destination.CHAPTERS.route)) { backStackEntry ->
            folder?.let {
                Screen(it)
            } ?: run {

            }
        }
    }

    @Composable
    override fun TopBar(navController: NavHostController) {
        NavigationTopBar(navController)
    }

    @Composable
    fun Screen(folder: MangaFolderDto) {
        val context = LocalContext.current
        val density = LocalDensity.current

        val imageRequest = remember(folder.coverUri) {
            val imageSize: Size = with(density) {
                Size(width = 120.dp.toPx().toInt(), height = 180.dp.toPx().toInt())
            }
            ImageRequest.Builder(context)
                .data(folder.coverUri)
                .size(resolver = SizeResolver(imageSize))
                .build()
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
                    .width(120.dp)
                    .height(180.dp)
                    .padding(vertical = 4.dp, horizontal = 8.dp),
            )

            val sortedChapters = folder.chapters.sortedBy { it.chapterSort }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f)
            ) {
                if (sortedChapters.isEmpty()) {
                    item {
                        Text(
                            text = "Nenhum capítulo encontrado",
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    items(items = sortedChapters) { chapter ->
                        ChapterItem(chapter = chapter, onClick = { /* abrir leitor */ })
                        Spacer(modifier = Modifier.height(height = 6.dp))
                    }
                }
            }
        }
    }
}