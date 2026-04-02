package br.acerola.manga.adapter.metadata.anilist.source

import arrow.core.Either
import br.acerola.manga.adapter.contract.provider.MetadataProvider
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.local.translator.remote.toViewDto
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.remote.anilist.AnilistApollo
import br.acerola.manga.remote.anilist.MediaDetailsQuery
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnilistMangaInfoSource @Inject constructor(
    @param:AnilistApollo private val apolloClient: ApolloClient,
) : MetadataProvider<MangaMetadataDto, String> {

    override suspend fun searchInfo(
        manga: String,
        limit: Int,
        offset: Int,
        onProgress: ((Int) -> Unit)?,
        vararg extra: String?
    ): Either<NetworkError, List<MangaMetadataDto>> = withContext(Dispatchers.IO) {
        val anilistId = manga.toIntOrNull()
            ?: return@withContext Either.Left(
                NetworkError.UnexpectedError(cause = Exception("Invalid AniList ID: $manga"))
            )

        Either.catch {
            val response = apolloClient
                .query(MediaDetailsQuery(id = Optional.present(anilistId)))
                .execute()

            if (response.hasErrors()) {
                val error = response.errors?.firstOrNull()
                AcerolaLogger.e("AnilistMangaInfoSource", "GraphQL error: ${error?.message}")
                if (response.data == null) {
                    throw Exception(error?.message ?: "GraphQL Error")
                }
            }

            val media = response.data?.Media
                ?: return@catch emptyList<MangaMetadataDto>()

            listOf(media.toViewDto())
        }.mapLeft { throwable ->
            when (throwable) {
                is com.apollographql.apollo.exception.ApolloNetworkException -> NetworkError.ConnectionFailed(cause = throwable)
                is com.apollographql.apollo.exception.ApolloHttpException -> NetworkError.HttpError(code = throwable.statusCode, cause = throwable)
                else -> NetworkError.UnexpectedError(cause = throwable)
            }
        }
    }

    override suspend fun saveInfo(
        manga: String,
        info: MangaMetadataDto
    ): Either<NetworkError, Unit> = Either.Right(Unit)
}
