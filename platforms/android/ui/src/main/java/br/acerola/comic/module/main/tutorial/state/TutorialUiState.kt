package br.acerola.comic.module.main.tutorial.state

data class TutorialUiState(
    val pageCount: Int = TutorialPage.entries.size,
)
