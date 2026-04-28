package br.acerola.comic.usecase.metadata

import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.adapter.contract.gateway.ComicSyncGateway
import br.acerola.comic.adapter.metadata.anilist.AnilistEngine
import br.acerola.comic.adapter.metadata.comicinfo.ComicInfoEngine
import br.acerola.comic.adapter.metadata.mangadex.MangadexEngine
import br.acerola.comic.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.error.message.LibrarySyncError
import javax.inject.Inject

class SyncComicMetadataUseCase
    @Inject
    constructor(
        @param:AnilistEngine private val anilistMangaRepo: ComicGateway<ComicMetadataDto>,
        @param:MangadexEngine private val mangadexMangaRepo: ComicGateway<ComicMetadataDto>,
        @param:MangadexEngine private val mangadexChapterRepo: ChapterGateway<ChapterRemoteInfoPageDto>,
        @param:ComicInfoEngine private val comicInfoMangaRepo: ComicSyncGateway,
        @param:ComicInfoEngine private val comicInfoChapterRepo: ChapterGateway<ChapterRemoteInfoPageDto>,
    ) {
        suspend fun syncFromMangadex(directoryId: Long): Either<LibrarySyncError, Unit> {
            // NOTE: mangaId aqui deve ser o ID do diretório local
            return mangadexMangaRepo.refreshManga(directoryId).onRight {
                mangadexChapterRepo.refreshComicChapters(directoryId)
            }
        }

        suspend fun syncFromComicInfo(directoryId: Long): Either<LibrarySyncError, Unit> {
            // NOTE: mangaId aqui deve ser o ID do diretório local
            return comicInfoMangaRepo.refreshManga(directoryId).onRight {
                comicInfoChapterRepo.refreshComicChapters(directoryId)
            }
        }

        suspend fun syncFromAnilist(directoryId: Long): Either<LibrarySyncError, Unit> = anilistMangaRepo.refreshManga(directoryId)
    }
