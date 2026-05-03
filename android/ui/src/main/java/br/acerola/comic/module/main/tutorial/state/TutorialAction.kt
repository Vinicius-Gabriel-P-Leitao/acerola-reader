package br.acerola.comic.module.main.tutorial.state

sealed interface TutorialAction {
    data object Complete : TutorialAction
}
