package br.acerola.comic.adapter.metadata.anilist.source

import arrow.core.Either
import br.acerola.comic.adapter.contract.provider.MetadataProvider
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.error.message.NetworkError
import br.acerola.comic.local.translator.remote.toViewDto
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.remote.anilist.AnilistApollo
import br.acerola.comic.remote.anilist.MediaSearchQuery
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnilistSearchByTitleSource
    @Inject
    constructor(
        @param:AnilistApollo private val apolloClient: ApolloClient,
    ) : MetadataProvider<ComicMetadataDto, String> {
        override suspend fun searchInfo(
            manga: String,
            limit: Int,
            offset: Int,
            onProgress: ((Int) -> Unit)?,
            vararg extra: String?,
        ): Either<NetworkError, List<ComicMetadataDto>> =
            withContext(Dispatchers.IO) {
                val page = if (limit > 0) (offset / limit) + 1 else 1

                Either
                    .catch {
                        val response =
                            apolloClient
                                .query(
                                    MediaSearchQuery(
                                        search = Optional.present(manga),
                                        page = Optional.present(page),
                                        perPage = Optional.present(limit),
                                    ),
                                ).execute()

                        if (response.hasErrors()) {
                            val error = response.errors?.firstOrNull()
                            AcerolaLogger.e("AnilistSearchByTitleSource", "GraphQL error: ${error?.message}")
                            // If we have errors but no data, treat as error
                            if (response.data == null) {
                                throw Exception(error?.message ?: "GraphQL Error")
                            }
                        }

                        response.data
                            ?.Page
                            ?.media
                            .orEmpty()
                            .mapNotNull { it?.toViewDto() }
                    }.mapLeft { throwable ->
                        when (throwable) {
                            is com.apollographql.apollo.exception.ApolloNetworkException -> NetworkError.ConnectionFailed(cause = throwable)
                            is com.apollographql.apollo.exception.ApolloHttpException ->
                                NetworkError.HttpError(
                                    code = throwable.statusCode,
                                    cause = throwable,
                                )
                            else -> NetworkError.UnexpectedError(cause = throwable)
                        }
                    }
            }

        override suspend fun saveInfo(
            manga: String,
            info: ComicMetadataDto,
        ): Either<NetworkError, Unit> = Either.Right(Unit)
    }
