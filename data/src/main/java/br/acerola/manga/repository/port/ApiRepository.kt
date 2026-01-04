package br.acerola.manga.repository.port


// NOTE: O vararg não é obrigatório por padrão.
interface ApiRepository {
    interface RemoteInfoOperations<R, P> {
        suspend fun searchInfo(manga: String, limit: Int = 10, offset: Int = 0, vararg extra: P?): List<R>
    }

    interface ArchiveOperations<P> {
        suspend fun searchCover(url: String, vararg extra: P?): ByteArray
    }
}