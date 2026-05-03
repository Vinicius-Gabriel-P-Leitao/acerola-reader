package br.acerola.comic.module.main.tutorial.state

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import br.acerola.comic.ui.R

enum class TutorialPage(
    @param:DrawableRes val imageRes: Int,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
) {
    WELCOME(
        imageRes = R.drawable.tutorial_slide_welcome,
        titleRes = R.string.tutorial_title_welcome,
        descriptionRes = R.string.tutorial_desc_welcome,
    ),
    FOLDER_STRUCTURE(
        imageRes = R.drawable.tutorial_slide_folder,
        titleRes = R.string.tutorial_title_folder,
        descriptionRes = R.string.tutorial_desc_folder,
    ),
    FILE_FORMATS(
        imageRes = R.drawable.tutorial_slide_files,
        titleRes = R.string.tutorial_title_files,
        descriptionRes = R.string.tutorial_desc_files,
    ),
}
