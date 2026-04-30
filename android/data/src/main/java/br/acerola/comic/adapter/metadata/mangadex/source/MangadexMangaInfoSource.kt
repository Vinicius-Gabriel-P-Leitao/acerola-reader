package br.acerola.comic.adapter.metadata.mangadex.source

import android.content.Context
import arrow.core.Either
import br.acerola.comic.adapter.contract.provider.MetadataProvider
import br.acerola.comic.config.network.safeApiCall
import br.acerola.comic.config.preference.MetadataPreference
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.error.message.NetworkError
import br.acerola.comic.local.translator.remote.toViewDto
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.pattern.LanguagePattern
import br.acerola.comic.remote.mangadex.api.MangadexMangaMetadataClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexMangaInfoSource
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val api: MangadexMangaMetadataClient,
    ) : MetadataProvider<ComicMetadataDto, String> {
        override suspend fun searchInfo(
            comic: String,
            limit: Int,
            offset: Int,
            onProgress: ((Int) -> Unit)?,
            vararg extra: String?,
        ): Either<NetworkError, List<ComicMetadataDto>> =
            safeApiCall {
                withContext(context = Dispatchers.IO) {
                    val preferredLanguage = MetadataPreference.metadataLanguageFlow(context).firstOrNull() ?: LanguagePattern.PT_BR.code
                    val languages = listOf(preferredLanguage)
                    AcerolaLogger.d(TAG, "Fetching comic: $comic, languages: $languages", LogSource.NETWORK)

                    if (UUID_REGEX.matches(comic)) {
                        AcerolaLogger.d(TAG, "Detected UUID — fetching comic by ID: $comic", LogSource.NETWORK)

                        val response = api.getMangaById(comicId = comic, languages = languages)
                        listOf(response.data.toViewDto(context, preferredLanguage))
                    } else {
                        AcerolaLogger.d(TAG, "Searching MangaDex for title: $comic (limit: $limit, offset: $offset)", LogSource.NETWORK)
                        val response = api.searchMangaByName(title = comic, limit = limit, offset = offset, languages = languages)
                        val list = response.data.map { it.toViewDto(context, preferredLanguage) }

                        AcerolaLogger.i(TAG, "Search completed: ${list.size} matches found for '$comic'", LogSource.NETWORK)
                        list
                    }
                }
            }.onLeft {
                AcerolaLogger.e(TAG, "MangaDex search failed for '$comic'", LogSource.NETWORK, throwable = null)
            }

        override suspend fun saveInfo(
            comic: String,
            info: ComicMetadataDto,
        ): Either<NetworkError, Unit> = Either.Right(Unit)

        companion object {
            private const val TAG = "MangadexMangaInfoRepository"
            private val UUID_REGEX =
                Regex(
                    "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
                    RegexOption.IGNORE_CASE,
                )
        }
    }
