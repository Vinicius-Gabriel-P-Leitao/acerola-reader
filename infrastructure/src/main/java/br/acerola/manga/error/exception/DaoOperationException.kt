package br.acerola.manga.error.exception

import androidx.annotation.StringRes
import br.acerola.manga.infrastructure.R

class DaoOperationException(
    @param:StringRes override val title: Int = R.string.title_dao_error,
    @param:StringRes override val description: Int = R.string.description_dao_error,
) : ApplicationException()