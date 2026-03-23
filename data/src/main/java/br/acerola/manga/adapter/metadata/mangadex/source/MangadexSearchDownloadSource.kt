package br.acerola.manga.adapter.metadata.mangadex.source

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import br.acerola.manga.adapter.contract.provider.DownloadProvider
import br.acerola.manga.adapter.contract.provider.MetadataProvider
import br.acerola.manga.adapter.metadata.mangadex.MangadexSource
import br.acerola.manga.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.service.download.DownloadManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexSearchDownloadSource @Inject constructor(
    @param:MangadexSource private val downloadManager: DownloadManager,
    @param:MangadexSource private val mangaInfoRepo: MetadataProvider<MangaMetadataDto, String>,
) : DownloadProvider {

    override suspend fun searchMangaByTitle(
        title: String,
        limit: Int
    ): Either<NetworkError, List<MangaMetadataDto>> {
        AcerolaLogger.d(TAG, "Searching MangaDex by title: $title", LogSource.NETWORK)
        return mangaInfoRepo.searchInfo(title, limit)
    }

    override suspend fun getMangaById(id: String): Either<NetworkError, MangaMetadataDto> {
        AcerolaLogger.d(TAG, "Fetching MangaDex manga by ID: $id", LogSource.NETWORK)
        return mangaInfoRepo.searchInfo(id, limit = 1).flatMap { list ->
            list.firstOrNull()?.right()
                ?: NetworkError.NotFound(cause = null).left()
        }
    }

    override suspend fun getChaptersByLanguage(
        mangaId: String,
        language: String,
        limit: Int,
        offset: Int
    ): Either<NetworkError, Pair<List<ChapterMetadataDto>, Int>> {
        AcerolaLogger.d(TAG, "Fetching chapters for manga $mangaId in $language (offset: $offset)", LogSource.NETWORK)
        return downloadManager.listChaptersByLanguage(mangaId, language, limit, offset).also { result ->
            result.onRight { (chapters, total) ->
                AcerolaLogger.i(TAG, "Got ${chapters.size} chapters (total: $total)", LogSource.NETWORK)
            }
        }
    }

    companion object {
        private const val TAG = "MangadexSearchDownloadSource"
    }
}
