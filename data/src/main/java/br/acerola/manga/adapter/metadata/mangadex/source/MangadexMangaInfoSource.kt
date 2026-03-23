package br.acerola.manga.adapter.metadata.mangadex.source

import android.content.Context
import arrow.core.Either
import br.acerola.manga.adapter.contract.provider.MetadataProvider
import br.acerola.manga.config.network.safeApiCall
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.local.translator.remote.toViewDto
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.remote.mangadex.api.MangadexMangaMetadataClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexMangaInfoSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val api: MangadexMangaMetadataClient
) : MetadataProvider<MangaMetadataDto, String> {

    override suspend fun searchInfo(
        manga: String, limit: Int, offset: Int, onProgress: ((Int) -> Unit)?, vararg extra: String?
    ): Either<NetworkError, List<MangaMetadataDto>> = safeApiCall {
        withContext(context = Dispatchers.IO) {
            if (UUID_REGEX.matches(manga)) {
                AcerolaLogger.d(TAG, "Detected UUID — fetching manga by ID: $manga", LogSource.NETWORK)

                val response = api.getMangaById(mangaId = manga)
                listOf(response.data.toViewDto(context))
            } else {
                AcerolaLogger.d(TAG, "Searching MangaDex for title: $manga (limit: $limit, offset: $offset)", LogSource.NETWORK)
                val response = api.searchMangaByName(title = manga, limit = limit, offset = offset)
                val list = response.data.map { it.toViewDto(context) }

                AcerolaLogger.i(TAG, "Search completed: ${list.size} matches found for '$manga'", LogSource.NETWORK)
                list
            }
        }
    }.onLeft {
        AcerolaLogger.e(TAG, "MangaDex search failed for '$manga'", LogSource.NETWORK, throwable = null)
    }

    override suspend fun saveInfo(manga: String, info: MangaMetadataDto): Either<NetworkError, Unit> {
        return Either.Right(Unit)
    }

    companion object {
        private const val TAG = "MangadexMangaInfoRepository"
        private val UUID_REGEX = Regex(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
            RegexOption.IGNORE_CASE
        )
    }
}
