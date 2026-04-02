package br.acerola.manga.adapter.contract.provider

import arrow.core.Either
import br.acerola.manga.error.message.NetworkError

interface MetadataProvider<R, P> {
    suspend fun searchInfo(
        manga: String, limit: Int = 10, offset: Int = 0, onProgress: ((Int) -> Unit)? = null, vararg extra: P?
    ): Either<NetworkError, List<R>>

    suspend fun saveInfo(manga: String, info: R): Either<NetworkError, Unit>
}
