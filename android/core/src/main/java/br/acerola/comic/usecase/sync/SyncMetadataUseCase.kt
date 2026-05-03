package br.acerola.comic.usecase.sync

import arrow.core.Either
import br.acerola.comic.error.message.LibrarySyncError
import br.acerola.comic.pattern.metadata.MetadataSource
import br.acerola.comic.usecase.AnilistCase
import br.acerola.comic.usecase.ComicInfoCase
import br.acerola.comic.usecase.MangadexCase
import br.acerola.comic.usecase.library.SyncLibraryUseCase as LegacySyncLibraryUseCase
import br.acerola.comic.usecase.metadata.SyncComicMetadataUseCase
import br.acerola.comic.worker.contract.SyncType
import javax.inject.Inject

class SyncMetadataUseCase
    @Inject
    constructor(
        private val syncComicMetadataUseCase: SyncComicMetadataUseCase,
        @AnilistCase private val anilistSyncUseCase: LegacySyncLibraryUseCase,
        @MangadexCase private val mangadexSyncUseCase: LegacySyncLibraryUseCase,
        @ComicInfoCase private val comicInfoSyncUseCase: LegacySyncLibraryUseCase,
    ) {
        suspend fun execute(
            source: MetadataSource,
            type: SyncType,
            directoryId: Long?,
        ): Either<LibrarySyncError, Unit> {
            return if (directoryId != null && directoryId != -1L) {
                // Single comic sync
                when (source) {
                    MetadataSource.MANGADEX -> syncComicMetadataUseCase.syncFromMangadex(directoryId)
                    MetadataSource.COMIC_INFO -> syncComicMetadataUseCase.syncFromComicInfo(directoryId)
                    MetadataSource.ANILIST -> syncComicMetadataUseCase.syncFromAnilist(directoryId)
                }
            } else {
                // Library-wide sync
                val useCase =
                    when (source) {
                        MetadataSource.MANGADEX -> mangadexSyncUseCase
                        MetadataSource.COMIC_INFO -> comicInfoSyncUseCase
                        MetadataSource.ANILIST -> anilistSyncUseCase
                    }

                when (type) {
                    SyncType.SYNC -> useCase.sync(baseUri = null)
                    SyncType.RESCAN -> useCase.rescan(baseUri = null)
                    else -> useCase.sync(baseUri = null)
                }
            }
        }
    }
