package br.acerola.comic.pattern

import br.acerola.comic.infra.R

enum class ComicStatusPattern(val stringRes: Int) {
    ONGOING(R.string.manga_status_ongoing),
    COMPLETED(R.string.manga_status_completed),
    CANCELLED(R.string.manga_status_cancelled),
    HIATUS(R.string.manga_status_hiatus),
    NOT_YET_RELEASED(R.string.manga_status_not_yet_released),
    UNKNOWN(R.string.manga_status_unknown);

    companion object {
        fun fromRawValue(value: String?): ComicStatusPattern {
            return when (value?.uppercase()) {
                "ONGOING", "RELEASING" -> ONGOING
                "COMPLETED", "FINISHED" -> COMPLETED
                "HIATUS" -> HIATUS
                "CANCELLED" -> CANCELLED
                "NOT_YET_RELEASED" -> NOT_YET_RELEASED
                else -> UNKNOWN
            }
        }
    }
}
