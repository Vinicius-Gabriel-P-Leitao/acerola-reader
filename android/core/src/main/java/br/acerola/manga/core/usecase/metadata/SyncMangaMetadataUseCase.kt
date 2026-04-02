package br.acerola.manga.core.usecase.metadata

import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.adapter.contract.gateway.ChapterGateway
import br.acerola.manga.adapter.contract.gateway.MangaGateway
import br.acerola.manga.adapter.contract.gateway.MangaSyncGateway
import br.acerola.manga.adapter.metadata.anilist.AnilistEngine
import br.acerola.manga.adapter.metadata.comicinfo.ComicInfoEngine
import br.acerola.manga.adapter.metadata.mangadex.MangadexEngine
import javax.inject.Inject

class SyncMangaMetadataUseCase @Inject constructor(
    @param:AnilistEngine private val anilistMangaRepo: MangaGateway<MangaMetadataDto>,
    @param:MangadexEngine private val mangadexMangaRepo: MangaGateway<MangaMetadataDto>,
    @param:MangadexEngine private val mangadexChapterRepo: ChapterGateway<ChapterRemoteInfoPageDto>,
    @param:ComicInfoEngine private val comicInfoMangaRepo: MangaSyncGateway,
    @param:ComicInfoEngine private val comicInfoChapterRepo: ChapterGateway<ChapterRemoteInfoPageDto>,
) {

    suspend fun syncFromMangadex(
        directoryId: Long,
    ): Either<LibrarySyncError, Unit> {
        // NOTE: mangaId aqui deve ser o ID do diretório local
        return mangadexMangaRepo.refreshManga(directoryId).onRight {
            mangadexChapterRepo.refreshMangaChapters(directoryId)
        }
    }

    suspend fun syncFromComicInfo(
        directoryId: Long
    ): Either<LibrarySyncError, Unit> {
        // NOTE: mangaId aqui deve ser o ID do diretório local
        return comicInfoMangaRepo.refreshManga(directoryId).onRight {
            comicInfoChapterRepo.refreshMangaChapters(directoryId)
        }
    }

    suspend fun syncFromAnilist(
        directoryId: Long
    ): Either<LibrarySyncError, Unit> {
        return anilistMangaRepo.refreshManga(directoryId)
    }
}
