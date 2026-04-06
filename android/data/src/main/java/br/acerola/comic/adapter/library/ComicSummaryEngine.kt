package br.acerola.comic.adapter.library

import br.acerola.comic.adapter.contract.gateway.ComicReadOnlyGateway
import br.acerola.comic.dto.view.ComicSummaryDto
import br.acerola.comic.local.dao.view.ComicSummaryDao
import br.acerola.comic.local.translator.ui.toViewDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComicSummaryEngine @Inject constructor(
    private val summaryDao: ComicSummaryDao
) : ComicReadOnlyGateway<ComicSummaryDto> {

    override fun observeLibrary(): Flow<List<ComicSummaryDto>> {
        return summaryDao.getAllMangaSummaries()
            .map { list -> list.map { it.toViewDto() } }
    }
}
