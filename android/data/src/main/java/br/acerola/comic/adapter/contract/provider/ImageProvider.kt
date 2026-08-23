package br.acerola.comic.adapter.contract.provider

import arrow.core.Either
import br.acerola.comic.error.message.NetworkError

interface ImageProvider<P> {
    suspend fun searchMedia(
        url: String,
        vararg extra: P?,
    ): Either<NetworkError, ByteArray>
}
