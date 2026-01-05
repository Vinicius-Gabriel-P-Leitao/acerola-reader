package br.acerola.manga.error.exception

import androidx.annotation.StringRes
import br.acerola.manga.infrastructure.R

class MangadexRequestException(
    @param:StringRes val title: Int = R.string.title_error_mangadex_sync,
    @param:StringRes val description: Int = R.string.message_error_mangadex_sync_unknown,
) : RuntimeException("Requisição do mangadex falhou.")