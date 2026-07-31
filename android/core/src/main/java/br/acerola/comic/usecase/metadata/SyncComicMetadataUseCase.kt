package br.acerola.comic.usecase.metadata

import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ChapterSyncGateway
import br.acerola.comic.adapter.contract.gateway.ComicSingleSyncGateway
import br.acerola.comic.adapter.metadata.anilist.AnilistEngine
import br.acerola.comic.adapter.metadata.comicinfo.ComicInfoEngine
import br.acerola.comic.adapter.metadata.mangadex.MangadexEngine
import br.acerola.comic.error.message.LibrarySyncError
import javax.inject.Inject

class SyncComicMetadataUseCase
    @Inject
    constructor(
        @param:AnilistEngine private val anilistMangaRepo: ComicSingleSyncGateway,
        @param:MangadexEngine private val mangadexMangaRepo: ComicSingleSyncGateway,
        @param:ComicInfoEngine private val comicInfoMangaRepo: ComicSingleSyncGateway,
    ) {
        suspend fun syncFromMangadex(directoryId: Long): Either<LibrarySyncError, Unit> = mangadexMangaRepo.refreshManga(directoryId)

        suspend fun syncFromComicInfo(directoryId: Long): Either<LibrarySyncError, Unit> = comicInfoMangaRepo.refreshManga(directoryId)

        suspend fun syncFromAnilist(directoryId: Long): Either<LibrarySyncError, Unit> = anilistMangaRepo.refreshManga(directoryId)
    }
