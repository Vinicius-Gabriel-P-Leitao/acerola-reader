package br.acerola.manga.repository.adapter.remote.anilist

import arrow.core.Either
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.local.mapper.toDto
import br.acerola.manga.remote.anilist.AnilistApollo
import br.acerola.manga.remote.anilist.MediaDetailsQuery
import br.acerola.manga.repository.port.RemoteInfoOperationsRepository
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnilistMangaInfoRepository @Inject constructor(
    @param:AnilistApollo private val apolloClient: ApolloClient,
) : RemoteInfoOperationsRepository<MangaRemoteInfoDto, String> {

    override suspend fun searchInfo(
        manga: String,
        limit: Int,
        offset: Int,
        onProgress: ((Int) -> Unit)?,
        vararg extra: String?
    ): Either<NetworkError, List<MangaRemoteInfoDto>> = withContext(Dispatchers.IO) {
        val anilistId = manga.toIntOrNull()
            ?: return@withContext Either.Left(
                NetworkError.UnexpectedError(cause = Exception("Invalid AniList ID: $manga"))
            )

        Either.catch {
            val response = apolloClient
                .query(MediaDetailsQuery(id = Optional.present(anilistId)))
                .execute()

            val media = response.data?.Media
                ?: return@catch emptyList<MangaRemoteInfoDto>()

            listOf(media.toDto())
        }.mapLeft { NetworkError.UnexpectedError(cause = it) }
    }

    override suspend fun saveInfo(
        manga: String,
        info: MangaRemoteInfoDto
    ): Either<NetworkError, Unit> = Either.Right(Unit)
}
