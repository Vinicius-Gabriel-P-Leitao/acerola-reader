package br.acerola.manga.repository.port

import arrow.core.Either
import br.acerola.manga.error.message.NetworkError

// NOTE: O vararg não é obrigatório por padrão.
interface ApiRepository {
    interface RemoteInfoOperations<R, P> {
        suspend fun searchInfo(
            manga: String,
            limit: Int = 10,
            offset: Int = 0,
            vararg extra: P?
        ): Either<NetworkError, List<R>>
    }

    interface ArchiveOperations<P> {
        suspend fun searchCover(url: String, vararg extra: P?): Either<NetworkError, ByteArray>
    }
}