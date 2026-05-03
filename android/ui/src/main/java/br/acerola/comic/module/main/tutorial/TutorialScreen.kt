package br.acerola.comic.module.main.tutorial

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import br.acerola.comic.common.ux.tokens.ShapeTokens
import br.acerola.comic.common.ux.tokens.SizeTokens
import br.acerola.comic.common.ux.tokens.SpacingTokens
import br.acerola.comic.module.main.Main
import br.acerola.comic.module.main.tutorial.state.TutorialPage
import br.acerola.comic.ui.R
import kotlinx.coroutines.launch

@Composable
fun Main.Tutorial.Template.Screen(
    viewModel: TutorialViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit,
) {
    val pages = TutorialPage.entries
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    val isFirstPage = pagerState.currentPage == 0
    val isLastPage = pagerState.currentPage == pages.lastIndex

    fun complete() {
        viewModel.markOnboardingCompleted()
        onNavigateToHome()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingTokens.Small, vertical = SpacingTokens.ExtraSmall),
            horizontalArrangement = Arrangement.End,
        ) {
            if (!isLastPage) {
                TextButton(onClick = ::complete) {
                    Text(text = stringResource(id = R.string.tutorial_action_skip))
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { pageIndex ->
            Main.Tutorial.Component.TutorialSlide(page = pages[pageIndex])
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = SpacingTokens.Large),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            pages.forEachIndexed { index, _ ->
                val isSelected = index == pagerState.currentPage
                Box(
                    modifier =
                        Modifier
                            .padding(horizontal = SpacingTokens.ExtraSmall)
                            .size(if (isSelected) SpacingTokens.Medium else SpacingTokens.Small)
                            .clip(ShapeTokens.Full)
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            ),
                )
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingTokens.Large)
                    .padding(bottom = SpacingTokens.Large),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                enabled = !isFirstPage,
            ) {
                Text(text = stringResource(id = R.string.tutorial_action_previous))
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
                ) {
                    Text(text = stringResource(id = R.string.tutorial_action_next))
                }
            }
        }
    }
}

@Composable
fun Main.Tutorial.Component.TutorialSlide(page: TutorialPage) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = SpacingTokens.Giant),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = page.imageRes),
            contentDescription = stringResource(id = R.string.tutorial_content_description_slide),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(SizeTokens.HistoryHeroHeight),
            contentScale = ContentScale.Fit,
        )

        Spacer(modifier = Modifier.height(SpacingTokens.Giant))

        Text(
            text = stringResource(id = page.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(SpacingTokens.Large))

        Text(
            text = stringResource(id = page.descriptionRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
