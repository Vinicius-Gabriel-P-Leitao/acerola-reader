package br.acerola.comic.usecase.metadata

import arrow.core.Either
import br.acerola.comic.error.message.IoError
import br.acerola.comic.local.dao.archive.VolumeArchiveDao
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.service.metadata.CoverExtractor
import javax.inject.Inject

class ExtractAllVolumeCoversUseCase
    @Inject
    constructor(
        private val volumeArchiveDao: VolumeArchiveDao,
        private val coverExtractor: CoverExtractor,
    ) {
        suspend operator fun invoke(comicId: Long): Either<IoError, Unit> {
            AcerolaLogger.i(TAG, "Starting extraction for all volumes of comic: $comicId", LogSource.USECASE)
            val volumes = volumeArchiveDao.getVolumesListByDirectoryId(comicId)
            AcerolaLogger.d(TAG, "Found ${volumes.size} volumes to process", LogSource.USECASE)

            var lastError: IoError? = null

            volumes.forEachIndexed { index, volume ->
                AcerolaLogger.v(TAG, "Processing volume ${index + 1}/${volumes.size}: ${volume.name}", LogSource.USECASE)
                coverExtractor.extractVolumeCover(comicId, volume.id).onLeft {
                    lastError = it
                    AcerolaLogger.e(TAG, "Failed to extract cover for volume ${volume.id}: $it", LogSource.USECASE)
                }
            }

            return if (lastError != null) Either.Left(lastError!!) else Either.Right(Unit)
        }

        companion object {
            private const val TAG = "ExtractAllVolumeCoversUseCase"
        }
    }
