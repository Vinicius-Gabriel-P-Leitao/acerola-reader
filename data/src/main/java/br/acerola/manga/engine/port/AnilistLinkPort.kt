package br.acerola.manga.engine.port

import arrow.core.Either
import br.acerola.manga.error.message.LibrarySyncError

// FIXME: Para que serve isso.
data class AnilistLink(val anilistId: String, val remoteInfoId: Long)

// FIXME: Para que serve isso.
interface AnilistLinkRepository {
    suspend fun getAnilistLink(directoryId: Long): Either<LibrarySyncError, AnilistLink>
}