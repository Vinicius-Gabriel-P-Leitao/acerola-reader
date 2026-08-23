package br.acerola.comic.pattern.metadata

import br.acerola.comic.infra.R

enum class ComicStatus(
    val stringRes: Int,
) {
    ONGOING(R.string.comic_status_ongoing),
    COMPLETED(R.string.comic_status_completed),
    CANCELLED(R.string.comic_status_cancelled),
    HIATUS(R.string.comic_status_hiatus),
    NOT_YET_RELEASED(R.string.comic_status_not_yet_released),
    UNKNOWN(R.string.comic_status_unknown),
    ;

    companion object {
        fun fromRawValue(value: String?): ComicStatus =
            when (value?.uppercase()) {
                "ONGOING", "RELEASING" -> ONGOING
                "COMPLETED", "FINISHED" -> COMPLETED
                "HIATUS" -> HIATUS
                "CANCELLED" -> CANCELLED
                "NOT_YET_RELEASED" -> NOT_YET_RELEASED
                else -> UNKNOWN
            }
    }
}
