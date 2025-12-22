package br.acerola.manga.ui.feature.chapter.activity

import android.content.Context
import android.os.Build
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.R
import br.acerola.manga.shared.dto.archive.ChapterFileDto
import br.acerola.manga.shared.dto.manga.MangaDto
import br.acerola.manga.ui.common.activity.BaseActivity
import br.acerola.manga.ui.common.component.ButtonType
import br.acerola.manga.ui.common.component.SmartButton
import br.acerola.manga.ui.common.layout.NavigationTopBar
import br.acerola.manga.ui.common.route.Destination
import br.acerola.manga.ui.common.viewmodel.library.archive.ChapterFileViewModel
import br.acerola.manga.ui.feature.chapter.component.ChapterItem
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChaptersActivity(
    override val startDestinationRes: Int = Destination.CHAPTERS.route
) : BaseActivity() {
    private val chapterViewModel: ChapterFileViewModel by viewModels()

    object ChapterExtra {
        const val MANGA = "MANGA"
    }

    enum class MainTab(@param:StringRes val titleRes: Int) {
        CHAPTERS(titleRes = R.string.title_chapter_tabs_chapters), SETTINGS(titleRes = R.string.title_chapter_tabs_settings)
    }

    val folder: MangaDto? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(ChapterExtra.MANGA, MangaDto::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra(ChapterExtra.MANGA)
        }
    }

    override fun NavGraphBuilder.setupNavGraph(context: Context, navController: NavHostController) {
        composable(route = context.getString(Destination.CHAPTERS.route)) {
            folder?.let {
                Screen(chapterViewModel, manga = it)
            }
        }
    }

    @Composable
    override fun TopBar(navController: NavHostController) {
        NavigationTopBar(navController)
    }

    @Composable
    fun Screen(
        chapterViewModel: ChapterFileViewModel,
        manga: MangaDto
    ) {
        var selectedTab by remember { mutableStateOf(value = MainTab.CHAPTERS) }

        LaunchedEffect(key1 = manga.folder.id) {
            chapterViewModel.init(folderId = manga.folder.id, firstPage = manga.folder.chapters)
        }

        val chapterPage by chapterViewModel.chapterPage.collectAsState()
        val total = chapterPage?.total ?: 0

        val chapters = (chapterPage?.items ?: emptyList()).sortedBy {
            it.chapterSort.replace(oldChar = ',', newChar = '.').toFloatOrNull() ?: 0f
        }

        // TODO: Trocar por cor do thema
        val backgroundColor = Color(color = 0xFF18181B)
        val primaryColor = Color(color = 0xFF6C5DD3)
        val textColor = Color.White
        val secondaryTextColor = Color.Gray

        Scaffold(
            containerColor = backgroundColor, contentColor = textColor
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                item {
                    MangaHeader(
                        manga = manga,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor
                    )
                }

                item {
                    MangaTabs(
                        totalChapters = total,
                        activeTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        textColor = textColor,
                        primaryColor = primaryColor,
                        secondaryTextColor = secondaryTextColor,
                    )
                }

                when (selectedTab) {
                    MainTab.CHAPTERS -> {
                        items(items = chapters, key = { it.id }) { chapter ->
                            ChapterListItem(
                                chapter = chapter, textColor = textColor, onClick = { /* Navegar */ })
                        }

                        if (chapters.size < total) {
                            item {
                                LaunchedEffect(Unit) { chapterViewModel.loadNextPage() }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(all = 16.dp),
                                ) {
                                    CircularProgressIndicator(color = primaryColor)
                                }
                            }
                        }
                    }

                    MainTab.SETTINGS -> {
                        item {
                            Text(text = "Texto de teste")
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(height = 24.dp)) }
            }
        }
    }


    @Composable
    fun MangaHeader(
        manga: MangaDto, textColor: Color, secondaryTextColor: Color
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = 420.dp)
        ) {
            val bannerModel = manga.folder.bannerUri ?: manga.folder.coverUri

            AsyncImage(
                contentDescription = null,
                contentScale = ContentScale.Crop,
                model = ImageRequest.Builder(context = LocalContext.current)
                    .data(data = bannerModel)
                    .crossfade(enable = true).build(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 300.dp)
                    .blur(radius = 10.dp)
                    .align(Alignment.TopCenter)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 300.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent, MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom
                ) {
                    AsyncImage(
                        contentDescription = "Cover",
                        contentScale = ContentScale.Crop,
                        model = ImageRequest.Builder(context = LocalContext.current)
                            .data(data = manga.folder.coverUri)
                            .crossfade(enable = true)
                            .build(),
                        modifier = Modifier
                            .clip(shape = RoundedCornerShape(size = 12.dp))
                            .width(width = 130.dp)
                            .background(Color.Gray)
                            .height(height = 190.dp)
                    )

                    Spacer(modifier = Modifier.width(width = 16.dp))

                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .height(height = 170.dp)
                            .weight(weight = 1f),
                    ) {
                        Text(
                            text = manga.metadata?.title ?: manga.folder.name,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold, color = textColor
                            ),
                        )

                        Spacer(modifier = Modifier.height(height = 4.dp))

                        Text(
                            // TODO: Nâo gerar string pois vai vim dos métadados de cada mangá
                            text = manga.metadata?.status ?: "Unknown",
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryTextColor
                        )

                        Spacer(modifier = Modifier.height(height = 8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Create,
                                modifier = Modifier.size(size = 16.dp),
                                tint = Color(color = 0xFFFFC107),
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(width = 4.dp))
                            Text(
                                // TODO: Verificar se consigo pegar status do mangá
                                text = manga.metadata?.authors?.name ?: "Unknown",
                                style = MaterialTheme.typography.labelLarge,
                                color = textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(height = 20.dp))

                // TODO: Criar função para pegar o ultimo antes de marcado como lido.
                SmartButton(
                    text = "Continue",
                    type = ButtonType.TEXT,
                    onClick = { /* TODO: Ação Continuar */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = 50.dp),
                )
            }
        }
    }

    @Composable
    fun MangaTabs(
        totalChapters: Int,
        activeTab: MainTab,
        onTabSelected: (MainTab) -> Unit,
        textColor: Color,
        secondaryTextColor: Color,
        primaryColor: Color
    ) {
        val tabs = MainTab.entries.toTypedArray()

        Row(
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 20.dp),
        ) {
            tabs.forEach { tab ->
                val isActive = tab == activeTab

                val title =
                    tab.takeIf { it == MainTab.CHAPTERS }?.let { stringResource(id = it.titleRes, totalChapters) }
                        ?: stringResource(id = tab.titleRes)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(end = 24.dp)
                        .clickable { onTabSelected(tab) },
                ) {
                    Text(
                        text = title,
                        color = if (isActive) textColor else secondaryTextColor,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        ),
                    )

                    if (isActive) {
                        Spacer(modifier = Modifier.height(height = 4.dp))
                        Box(
                            modifier = Modifier
                                .width(width = 20.dp)
                                .height(height = 3.dp)
                                .background(primaryColor, shape = RoundedCornerShape(size = 2.dp))
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ChapterListItem(
        chapter: ChapterFileDto, textColor: Color, onClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(size = 0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            ChapterItem(chapter, textColor, onClick)
        }
    }
}
