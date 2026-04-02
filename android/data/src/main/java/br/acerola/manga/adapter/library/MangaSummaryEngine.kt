package br.acerola.manga.adapter.library

import br.acerola.manga.adapter.contract.gateway.MangaReadOnlyGateway
import br.acerola.manga.dto.view.MangaSummaryDto
import br.acerola.manga.local.dao.view.MangaSummaryDao
import br.acerola.manga.local.translator.ui.toViewDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaSummaryEngine @Inject constructor(
    private val summaryDao: MangaSummaryDao
) : MangaReadOnlyGateway<MangaSummaryDto> {

    override fun observeLibrary(): Flow<List<MangaSummaryDto>> {
        return summaryDao.getAllMangaSummaries()
            .map { list -> list.map { it.toViewDto() } }
    }
}
