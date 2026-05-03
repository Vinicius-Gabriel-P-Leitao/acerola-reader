package br.acerola.comic.usecase.metadata

import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ChapterSyncGateway
import br.acerola.comic.adapter.contract.gateway.ComicSingleSyncGateway
import br.acerola.comic.adapter.metadata.anilist.AnilistEngine
import br.acerola.comic.adapter.metadata.comicinfo.ComicInfoEngine
import br.acerola.comic.adapter.metadata.mangadex.MangadexEngine
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.error.message.LibrarySyncError
import javax.inject.Inject

class SyncComicMetadataUseCase
    @Inject
    constructor(
        @param:AnilistEngine private val anilistMangaRepo: ComicSingleSyncGateway,
        @param:MangadexEngine private val mangadexMangaRepo: ComicSingleSyncGateway,
        @param:MangadexEngine private val mangadexChapterRepo: ChapterSyncGateway,
        @param:ComicInfoEngine private val comicInfoMangaRepo: ComicSingleSyncGateway,
        @param:ComicInfoEngine private val comicInfoChapterRepo: ChapterSyncGateway,
    ) {
        suspend fun syncFromMangadex(directoryId: Long): Either<LibrarySyncError, Unit> {
            // NOTE: comicId aqui deve ser o ID do diretório local
            return mangadexMangaRepo.refreshManga(directoryId).onRight {
                mangadexChapterRepo.refreshComicChapters(directoryId)
            }
        }

        suspend fun syncFromComicInfo(directoryId: Long): Either<LibrarySyncError, Unit> {
            // NOTE: comicId aqui deve ser o ID do diretório local
            return comicInfoMangaRepo.refreshManga(directoryId).onRight {
                comicInfoChapterRepo.refreshComicChapters(directoryId)
            }
        }

        suspend fun syncFromAnilist(directoryId: Long): Either<LibrarySyncError, Unit> = anilistMangaRepo.refreshManga(directoryId)
    }
