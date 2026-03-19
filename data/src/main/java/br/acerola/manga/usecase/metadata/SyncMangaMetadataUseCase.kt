package br.acerola.manga.usecase.metadata

import arrow.core.Either
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.repository.di.AnilistFsOps
import br.acerola.manga.repository.di.ComicInfoFsOps
import br.acerola.manga.repository.di.MangadexFsOps
import br.acerola.manga.repository.port.ChapterManagementRepository
import br.acerola.manga.repository.port.MangaManagementRepository
import javax.inject.Inject

class SyncMangaMetadataUseCase @Inject constructor(
    @param:AnilistFsOps private val anilistMangaRepo: MangaManagementRepository<MangaRemoteInfoDto>,
    @param:MangadexFsOps private val mangadexMangaRepo: MangaManagementRepository<MangaRemoteInfoDto>,
    @param:MangadexFsOps private val mangadexChapterRepo: ChapterManagementRepository<ChapterRemoteInfoPageDto>,
    @param:ComicInfoFsOps private val comicInfoMangaRepo: MangaManagementRepository<MangaRemoteInfoDto>,
    @param:ComicInfoFsOps private val comicInfoChapterRepo: ChapterManagementRepository<ChapterRemoteInfoPageDto>,
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
