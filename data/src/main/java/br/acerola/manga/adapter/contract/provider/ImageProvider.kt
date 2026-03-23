package br.acerola.manga.adapter.contract.provider

import arrow.core.Either
import br.acerola.manga.error.message.NetworkError

interface ImageProvider<P> {
    suspend fun searchMedia(url: String, vararg extra: P?): Either<NetworkError, ByteArray>
}
