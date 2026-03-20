package br.acerola.manga.adapter.contract

import arrow.core.Either
import br.acerola.manga.error.message.NetworkError

interface ImageFetchPort<P> {
    suspend fun searchCover(url: String, vararg extra: P?): Either<NetworkError, ByteArray>
}
