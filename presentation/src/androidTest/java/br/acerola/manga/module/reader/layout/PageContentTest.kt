package br.acerola.manga.module.reader.layout

import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.config.preference.ReadingMode
import br.acerola.manga.module.reader.Reader
import br.acerola.manga.module.reader.state.ReaderUiState
import org.junit.Rule
import org.junit.Test

class PageContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `deve renderizar conteudo do leitor sem erros`() {
        val uiState = ReaderUiState(
            readingMode = ReadingMode.HORIZONTAL,
            pageCount = 10
        )

        composeTestRule.setContent {
            Reader.Layout.PageContent(
                uiState = uiState,
                onUiToggle = {},
                onPrevClick = {},
                onNextClick = {},
                onPageRequest = {},
                onZoomChange = {},
                onWebtoonScroll = { _, _ -> }
            )
        }

        composeTestRule.waitForIdle()
    }
}
