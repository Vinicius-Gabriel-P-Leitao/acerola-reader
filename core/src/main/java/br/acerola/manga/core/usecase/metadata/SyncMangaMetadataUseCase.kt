package br.acerola.manga.core.usecase.metadata

import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.engine.di.AnilistFsOps
import br.acerola.manga.engine.di.ComicInfoFsOps
import br.acerola.manga.engine.di.MangadexFsOps
import br.acerola.manga.engine.port.ChapterPort
import br.acerola.manga.engine.port.MangaPort
import javax.inject.Inject

class SyncMangaMetadataUseCase @Inject constructor(
    @param:AnilistFsOps private val anilistMangaRepo: MangaPort<MangaRemoteInfoDto>,
    @param:MangadexFsOps private val mangadexMangaRepo: MangaPort<MangaRemoteInfoDto>,
    @param:MangadexFsOps private val mangadexChapterRepo: ChapterPort<ChapterRemoteInfoPageDto>,
    @param:ComicInfoFsOps private val comicInfoMangaRepo: MangaPort<MangaRemoteInfoDto>,
    @param:ComicInfoFsOps private val comicInfoChapterRepo: ChapterPort<ChapterRemoteInfoPageDto>,
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
