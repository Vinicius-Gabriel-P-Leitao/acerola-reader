package br.acerola.manga.domain.service.api.mangadex

import br.acerola.manga.domain.service.api.ApiPort

class MangaDexFetchCoverService: ApiPort.ArchiveOperations<String> {
    override suspend fun searchCover(search: String, vararg extra: String?): ByteArray {
        TODO("Not yet implemented")
    }
}