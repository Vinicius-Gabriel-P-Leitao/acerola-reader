package br.acerola.comic.module.main.tutorial

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import br.acerola.comic.common.ux.tokens.ShapeTokens
import br.acerola.comic.common.ux.tokens.SpacingTokens
import br.acerola.comic.common.viewmodel.archive.FileSystemAccessViewModel
import br.acerola.comic.common.viewmodel.theme.ThemeViewModel
import br.acerola.comic.module.main.Main
import br.acerola.comic.module.main.config.component.SelectComicDirectory
import br.acerola.comic.module.main.config.component.ThemeSettings
import br.acerola.comic.module.main.tutorial.state.TutorialPage
import br.acerola.comic.ui.R
import kotlinx.coroutines.launch

@Composable
fun Main.Tutorial.Template.Screen(
    viewModel: TutorialViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    fileSystemAccessViewModel: FileSystemAccessViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit,
) {
    val pages = TutorialPage.entries
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    val isFirstPage = pagerState.currentPage == 0
    val isLastPage = pagerState.currentPage == pages.lastIndex

    val folderName by fileSystemAccessViewModel.folderName.collectAsState()
    val folderUri = fileSystemAccessViewModel.folderUri
    val canProceedSettings = !folderName.isNullOrEmpty()

    fun complete() {
        viewModel.markOnboardingCompleted()
        onNavigateToHome()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SpacingTokens.Large, horizontal = SpacingTokens.Large),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            pages.forEachIndexed { index, _ ->
                val isSelected = index == pagerState.currentPage
                val isPast = index < pagerState.currentPage

                val bgColor = if (isSelected || isPast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                val textColor = if (isSelected || isPast) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(ShapeTokens.Full)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "${index + 1}", color = textColor, fontWeight = FontWeight.Bold)
                }

                if (index < pages.lastIndex) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(2.dp)
                            .background(if (isSelected || isPast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = false
        ) { pageIndex ->
            when (pageIndex) {
                0 -> WelcomeSlide()
                1 -> FormatsSlide()
                2 -> SettingsSlide(themeViewModel, fileSystemAccessViewModel, folderName)
                3 -> CompleteSlide()
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.Large)
                .padding(bottom = SpacingTokens.Large),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isFirstPage) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                ) {
                    Text(text = stringResource(id = R.string.tutorial_action_previous))
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (isLastPage) {
                Button(onClick = ::complete) {
                    Text(text = stringResource(id = R.string.tutorial_action_finish))
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    enabled = if (pagerState.currentPage == 2) canProceedSettings else true
                ) {
                    Text(text = stringResource(id = R.string.tutorial_action_next))
                }
            }
        }
    }
}

@Composable
fun WelcomeSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SpacingTokens.Giant),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Acerola",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(SpacingTokens.Medium))
        Text(
            text = stringResource(id = R.string.tutorial_desc_welcome),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun FormatsSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SpacingTokens.Giant),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(id = R.string.tutorial_title_files),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(SpacingTokens.Giant))

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Text(
                text = "CBZ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(SpacingTokens.Large)
            )
        }

        Spacer(modifier = Modifier.height(SpacingTokens.Medium))

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Text(
                text = "CBR",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(SpacingTokens.Large)
            )
        }

        Spacer(modifier = Modifier.height(SpacingTokens.Medium))

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(SpacingTokens.Large)) {
                Text(
                    text = "PDF",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(SpacingTokens.ExtraSmall))
                Text(
                    text = stringResource(id = R.string.tutorial_desc_files),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsSlide(
    themeViewModel: ThemeViewModel,
    fileSystemAccessViewModel: FileSystemAccessViewModel,
    folderName: String?
) {
    val selectedTheme by themeViewModel.currentTheme.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SpacingTokens.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(id = R.string.tutorial_title_settings),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(SpacingTokens.Large))
        
        Main.Config.Component.ThemeSettings(
            currentTheme = selectedTheme,
            onThemeChange = { themeViewModel.setTheme(it) },
        )
        
        Spacer(modifier = Modifier.height(SpacingTokens.Large))
        
        Main.Config.Component.SelectComicDirectory(
            folderName = folderName,
            onFolderSelected = { fileSystemAccessViewModel.saveFolderUri(it) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun CompleteSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SpacingTokens.Giant),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(SpacingTokens.Large))
        Text(
            text = stringResource(id = R.string.tutorial_title_complete),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(SpacingTokens.Medium))
        Text(
            text = stringResource(id = R.string.tutorial_desc_complete),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(SpacingTokens.Large))
        Text(
            text = stringResource(id = R.string.tutorial_note_complete),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
