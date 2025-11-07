package br.acerola.manga.ui.feature.chapters.activity

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.domain.database.AcerolaDatabase
import br.acerola.manga.domain.service.archive.ArchiveMangaService
import br.acerola.manga.shared.dto.archive.ChapterFileDto
import br.acerola.manga.shared.permission.FolderAccessManager
import br.acerola.manga.shared.route.Destination
import br.acerola.manga.ui.common.activity.BaseActivity
import br.acerola.manga.ui.common.component.ButtonType
import br.acerola.manga.ui.common.component.CardType
import br.acerola.manga.ui.common.component.SmartButton
import br.acerola.manga.ui.common.component.SmartCard
import br.acerola.manga.ui.common.layout.NavigationTopBar
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModel
import br.acerola.manga.ui.common.viewmodel.archive.folder.FolderAccessViewModelFactory
import br.acerola.manga.ui.feature.main.home.viewmodel.MangaLibraryViewModel
import br.acerola.manga.ui.feature.main.home.viewmodel.MangaLibraryViewModelFactory

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
                application, folderAccessViewModel = folderAccessViewModel, archiveService = ArchiveMangaService(
                    context = applicationContext,
                    folderDao = database.mangaFolderDao(),
                    chapterDao = database.chapterFileDao()
                )
            )
        )[MangaLibraryViewModel::class.java]
    }

    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        composable(route = context.getString(Destination.CHAPTERS.route)) { backStackEntry ->
            val folderId = intent?.getLongExtra("folderId", -1L) ?: -1L
            // NOTE: Pega o id do intent para fazer a busca dos capilutos
            if (folderId != -1L) mangaLibraryViewModel.selectFolder(folderId)

            Screen()
        }
    }

    @Composable
    override fun TopBar(navController: NavHostController) {
        NavigationTopBar(navController)
    }

    @Composable
    fun Screen() {
        val chapters by mangaLibraryViewModel.chapters.collectAsState()

        Column() {
            if (chapters.isEmpty()) {
                Text(text = "Nenhum capítulo encontrado")
            } else {
                chapters.forEach { chapter ->
                    ChapterItem(chapter = chapter, onClick = { /* abrir leitor */ })
                    Spacer(modifier = Modifier.height(height = 2.dp))
                }
            }
        }
    }

    @Composable
    fun ChapterItem(
        chapter: ChapterFileDto, onClick: (ChapterFileDto) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SmartCard(
                type = CardType.CONTENT, modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                ) {
                    Text(
                        text = chapter.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    SmartButton(
                        type = ButtonType.ICON, onClick = { onClick(chapter) }, modifier = Modifier.size(36.dp)
                    ) { Icon(Icons.Default.FavoriteBorder, contentDescription = "Ler capítulo") }
                }
            }
        }
    }
}