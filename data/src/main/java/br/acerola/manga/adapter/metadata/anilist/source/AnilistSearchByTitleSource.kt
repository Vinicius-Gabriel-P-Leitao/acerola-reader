package br.acerola.manga.adapter.metadata.anilist.source

import arrow.core.Either
import br.acerola.manga.adapter.contract.RemoteInfoOperationsPort
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.local.translator.toDto
import br.acerola.manga.remote.anilist.AnilistApollo
import br.acerola.manga.remote.anilist.MediaSearchQuery
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnilistSearchByTitleSource @Inject constructor(
    @param:AnilistApollo private val apolloClient: ApolloClient,
) : RemoteInfoOperationsPort<MangaRemoteInfoDto, String> {

    override suspend fun searchInfo(
        manga: String,
        limit: Int,
        offset: Int,
        onProgress: ((Int) -> Unit)?,
        vararg extra: String?
    ): Either<NetworkError, List<MangaRemoteInfoDto>> = withContext(Dispatchers.IO) {
        val page = if (limit > 0) (offset / limit) + 1 else 1

        Either.catch {
            val response = apolloClient
                .query(
                    MediaSearchQuery(
                        search = Optional.present(manga),
                        page = Optional.present(page),
                        perPage = Optional.present(limit)
                    )
                )
                .execute()

            response.data?.Page?.media.orEmpty()
                .mapNotNull { it?.toDto() }
        }.mapLeft { NetworkError.UnexpectedError(cause = it) }
    }

    override suspend fun saveInfo(
        manga: String,
        info: MangaRemoteInfoDto
    ): Either<NetworkError, Unit> = Either.Right(Unit)
}
