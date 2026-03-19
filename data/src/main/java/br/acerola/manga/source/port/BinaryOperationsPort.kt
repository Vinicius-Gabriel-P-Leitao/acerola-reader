package br.acerola.manga.source.port

import arrow.core.Either
import br.acerola.manga.error.message.NetworkError

interface BinaryOperationsPort<P> {
    suspend fun searchCover(url: String, vararg extra: P?): Either<NetworkError, ByteArray>
}