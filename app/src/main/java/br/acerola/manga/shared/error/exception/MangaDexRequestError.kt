package br.acerola.manga.shared.error.exception

import androidx.annotation.StringRes
import br.acerola.manga.R

class MangaDexRequestError(
    @param:StringRes override val title: Int = R.string.title_error_mangadex_sync,
    @param:StringRes override val description: Int = R.string.message_error_mangadex_sync_unknown,
) : ApplicationException()