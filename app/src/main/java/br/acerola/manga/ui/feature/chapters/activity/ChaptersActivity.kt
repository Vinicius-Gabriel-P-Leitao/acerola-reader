package br.acerola.manga.ui.feature.chapters.activity

import android.content.Context
import android.os.Build
import androidx.activity.viewModels
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import br.acerola.manga.R
import br.acerola.manga.domain.database.dao.database.AcerolaDatabase
import br.acerola.manga.domain.service.library.chapter.ChapterFileService
import br.acerola.manga.shared.dto.archive.ChapterFileDto
import br.acerola.manga.shared.dto.archive.MangaFolderDto
import br.acerola.manga.shared.route.Destination
import br.acerola.manga.ui.common.activity.BaseActivity
import br.acerola.manga.ui.common.component.ButtonType
import br.acerola.manga.ui.common.component.SmartButton
import br.acerola.manga.ui.common.layout.NavigationTopBar
import br.acerola.manga.ui.common.viewmodel.library.ChapterViewModel
import br.acerola.manga.ui.common.viewmodel.library.ChapterViewModelFactory
import br.acerola.manga.ui.feature.chapters.component.ChapterItem
import coil.compose.AsyncImage
import coil.request.ImageRequest

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
        composable(route = context.getString(Destination.CHAPTERS.route)) {
            folder?.let {
                Screen(chapterViewModel, folder = it)
            }
        }
    }

    @Composable
    override fun TopBar(navController: NavHostController) {
        NavigationTopBar(navController)
    }

    @Composable
    fun Screen(
        chapterViewModel: ChapterViewModel, folder: MangaFolderDto
    ) {
        LaunchedEffect(key1 = folder.id) {
            chapterViewModel.init(folderId = folder.id, firstPage = folder.chapters)
        }

        val chapterPage by chapterViewModel.chapterPage.collectAsState()
        val chapters = chapterPage?.items ?: emptyList()
        val total = chapterPage?.total ?: 0

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
                        folder = folder,
                        textColor = textColor,
                        primaryColor = primaryColor,
                        secondaryTextColor = secondaryTextColor
                    )
                }

                item {
                    MangaTabs(
                        totalChapters = total,
                        // TODO: USar a string gerada
                        activeTab = "Capitulos",
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        primaryColor = primaryColor
                    )
                }

                items(items = chapters, key = { it.id }) { chapter ->
                    ChapterListItem(
                        chapter = chapter, textColor = textColor, onClick = { /* TODO: Navegar para Leitor */ })
                }

                if (chapters.size < total) {
                    item {
                        LaunchedEffect(key1 = Unit) { chapterViewModel.loadNextPage() }
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

                item { Spacer(modifier = Modifier.height(height = 24.dp)) }
            }
        }
    }


    @Composable
    fun MangaHeader(
        folder: MangaFolderDto, primaryColor: Color, textColor: Color, secondaryTextColor: Color
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = 420.dp)
        ) {
            val bannerModel = folder.bannerUri ?: folder.coverUri

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
                                Color.Transparent,
                                MaterialTheme.colorScheme.background
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
                            .data(data = folder.coverUri)
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
                            text = folder.name,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold, color = textColor
                            ),
                        )

                        Spacer(modifier = Modifier.height(height = 4.dp))

                        Text(
                            // TODO: Nâo gerar string pois vai vim dos métadados de cada mangá
                            text = "Unknown Author",
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryTextColor
                        )

                        Spacer(modifier = Modifier.height(height = 8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                modifier = Modifier.size(size = 16.dp),
                                tint = Color(color = 0xFFFFC107),
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(width = 4.dp))
                            Text(
                                // TODO: Nâo gerar string pois vai vim dos métadados de cada mangá
                                text = "Ongoing", style = MaterialTheme.typography.labelLarge, color = textColor
                            )
                        }

                        Spacer(modifier = Modifier.height(height = 8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(size = 16.dp)
                            )

                            Text(text = " 8.3", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.width(width = 16.dp))

                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(size = 16.dp)
                            )

                            Text(text = " 65k", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
        totalChapters: Int, activeTab: String, textColor: Color, secondaryTextColor: Color, primaryColor: Color
    ) {
        // TODO: Criar string
        val tabs = listOf("Capitulos ($totalChapters)", "Configurações")

        Row(
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 20.dp),
        ) {
            tabs.forEach { tabName ->
                val isActive = tabName.startsWith(prefix = activeTab)

                Column(
                    modifier = Modifier.padding(end = 24.dp), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tabName,
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
                                .background(primaryColor, shape = RoundedCornerShape(2.dp))
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
