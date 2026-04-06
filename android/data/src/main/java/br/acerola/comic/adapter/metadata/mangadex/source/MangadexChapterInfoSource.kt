package br.acerola.comic.adapter.metadata.mangadex.source

import android.content.Context
import arrow.core.Either
import br.acerola.comic.adapter.contract.provider.MetadataProvider
import br.acerola.comic.config.network.safeApiCall
import br.acerola.comic.config.preference.MetadataPreference
import br.acerola.comic.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.comic.error.message.NetworkError
import br.acerola.comic.local.translator.remote.toViewDto
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.pattern.LanguagePattern
import br.acerola.comic.remote.mangadex.api.MangadexChapterMetadataClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexChapterInfoSource @Inject constructor(
    private val api: MangadexChapterMetadataClient,
    @param:ApplicationContext private val context: Context
) : MetadataProvider<ChapterMetadataDto, String> {

    override suspend fun searchInfo(
        manga: String, limit: Int, offset: Int, onProgress: ((Int) -> Unit)?, vararg extra: String?
    ): Either<NetworkError, List<ChapterMetadataDto>> = withContext(context = Dispatchers.IO) {
        val preferredLanguage = MetadataPreference.metadataLanguageFlow(context).firstOrNull() ?: LanguagePattern.PT_BR.code
        val languagesList = listOf(preferredLanguage)
        
        AcerolaLogger.d(TAG, "GET /comic/$manga/feed initiated (offset: $offset) for languages: $languagesList", LogSource.NETWORK)
        
        val allChapters = mutableListOf<ChapterMetadataDto>()
        val semaphore = Semaphore(permits = 3)
        var currentOffset = offset
        var error: NetworkError? = null

        val initialResponseResult = safeApiCall { api.getMangaFeed(mangaId = manga, languages = languagesList, limit = 1, offset = 0) }
        val totalChapters = initialResponseResult.getOrNull()?.total ?: 0

        while (true) {
            if (totalChapters > 0 && onProgress != null) {
                val progress = ((currentOffset.toFloat() / totalChapters.toFloat()) * 100).toInt()
                onProgress(progress)
            }

            val responseFeedResult = safeApiCall {
                api.getMangaFeed(mangaId = manga, languages = languagesList, limit = limit, offset = currentOffset)
            }

            if (responseFeedResult is Either.Left) {
                AcerolaLogger.e(TAG, "Failed to fetch chapter feed for comic: $manga", LogSource.NETWORK)
                error = responseFeedResult.value
                break
            }

            val responseFeed = responseFeedResult.getOrNull()!!
            AcerolaLogger.d(TAG, "Feed received: ${responseFeed.data.size} chapters in this batch", LogSource.NETWORK)

            val processedBatch = responseFeed.data.map { item ->
                async {
                    semaphore.withPermit {
                        val sourceResult = safeApiCall { api.getChapterImages(chapterId = item.id) }
                        val source = sourceResult.getOrNull()
                        item.toViewDto(source)
                    }
                }
            }.awaitAll()

            allChapters.addAll(elements = processedBatch)
            currentOffset += 100

            if (currentOffset >= responseFeed.total) break
        }

        if (error != null && allChapters.isEmpty()) {
            Either.Left(value = error)
        } else {
            AcerolaLogger.i(TAG, "Successfully fetched total of ${allChapters.size} chapters for comic $manga", LogSource.NETWORK)
            Either.Right(value = allChapters)
        }
    }

    override suspend fun saveInfo(manga: String, info: ChapterMetadataDto): Either<NetworkError, Unit> =
        Either.Right(Unit)

    companion object {
        private const val TAG = "MangadexChapterInfoSource"
    }
}
