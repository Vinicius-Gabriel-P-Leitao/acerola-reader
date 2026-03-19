package br.acerola.manga.repository.adapter.remote.mangadex.download

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.repository.di.Mangadex
import br.acerola.manga.repository.port.DownloadRepository
import br.acerola.manga.repository.port.RemoteInfoOperationsRepository
import br.acerola.manga.service.download.ChapterDownloadService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexSearchDownloadRepository @Inject constructor(
    private val chapterDownloadService: ChapterDownloadService,
    @param:Mangadex private val mangaInfoRepo: RemoteInfoOperationsRepository<MangaRemoteInfoDto, String>,
) : DownloadRepository {

    override suspend fun searchMangaByTitle(
        title: String,
        limit: Int
    ): Either<NetworkError, List<MangaRemoteInfoDto>> {
        AcerolaLogger.d(TAG, "Searching MangaDex by title: $title", LogSource.NETWORK)
        return mangaInfoRepo.searchInfo(title, limit)
    }

    override suspend fun getMangaById(id: String): Either<NetworkError, MangaRemoteInfoDto> {
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
    ): Either<NetworkError, Pair<List<ChapterRemoteInfoDto>, Int>> {
        AcerolaLogger.d(TAG, "Fetching chapters for manga $mangaId in $language (offset: $offset)", LogSource.NETWORK)
        return chapterDownloadService.listChaptersByLanguage(mangaId, language, limit, offset).also { result ->
            result.onRight { (chapters, total) ->
                AcerolaLogger.i(TAG, "Got ${chapters.size} chapters (total: $total)", LogSource.NETWORK)
            }
        }
    }

    companion object {
        private const val TAG = "MangadexSearchDownloadRepository"
    }
}
