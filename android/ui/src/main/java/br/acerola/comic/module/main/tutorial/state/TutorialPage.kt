package br.acerola.comic.module.main.tutorial.state

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import br.acerola.comic.ui.R

enum class TutorialPage(
    @param:StringRes val titleRes: Int,
) {
    WELCOME(
        titleRes = R.string.tutorial_title_welcome,
    ),
    FILE_FORMATS(
        titleRes = R.string.tutorial_title_files,
    ),
    SETTINGS(
        titleRes = R.string.tutorial_title_settings,
    ),
    COMPLETE(
        titleRes = R.string.tutorial_title_complete,
    ),
}
