package br.acerola.manga.core.usecase.metadata

import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.adapter.di.AnilistEngine
import br.acerola.manga.adapter.di.ComicInfoEngine
import br.acerola.manga.adapter.di.MangadexEngine
import br.acerola.manga.adapter.port.ChapterPort
import br.acerola.manga.adapter.port.MangaPort
import javax.inject.Inject

class SyncMangaMetadataUseCase @Inject constructor(
    @param:AnilistEngine private val anilistMangaRepo: MangaPort<MangaRemoteInfoDto>,
    @param:MangadexEngine private val mangadexMangaRepo: MangaPort<MangaRemoteInfoDto>,
    @param:MangadexEngine private val mangadexChapterRepo: ChapterPort<ChapterRemoteInfoPageDto>,
    @param:ComicInfoEngine private val comicInfoMangaRepo: MangaPort<MangaRemoteInfoDto>,
    @param:ComicInfoEngine private val comicInfoChapterRepo: ChapterPort<ChapterRemoteInfoPageDto>,
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
