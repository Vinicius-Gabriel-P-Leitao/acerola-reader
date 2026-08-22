package br.acerola.comic.service.metadata

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.local.dao.metadata.ComicMetadataDao
import br.acerola.comic.local.entity.archive.ComicDirectory
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.pattern.media.MediaFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataCleaner
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val directoryDao: ComicDirectoryDao,
        private val comicMetadataDao: ComicMetadataDao,
    ) {
        // NOTE: author/genre/mangadex_source/anilist_source cascade off comic_metadata via
        // Room's FK onDelete=CASCADE, so deleting the metadata row is enough to wipe them.
        suspend fun clearMetadata(directoryId: Long) =
            withContext(Dispatchers.IO) {
                AcerolaLogger.i(TAG, "Clearing metadata for comic directory: $directoryId", LogSource.SERVICE)

                comicMetadataDao.deleteByDirectoryId(directoryId)

                val directory = directoryDao.getDirectoryById(directoryId) ?: return@withContext
                directoryDao.update(directory.copy(cover = null, banner = null))

                deleteMetadataFiles(directory)
            }

        private fun deleteMetadataFiles(directory: ComicDirectory) {
            try {
                val folderDoc = DocumentFile.fromTreeUri(context, directory.path.toUri()) ?: return
                folderDoc.listFiles().forEach { file ->
                    val name = file.name ?: return@forEach
                    if (name.equals("ComicInfo.xml", ignoreCase = true) || MediaFile.isCover(name) || MediaFile.isBanner(name)) {
                        file.delete()
                    }
                }
            } catch (exception: Exception) {
                AcerolaLogger.e(
                    TAG,
                    "Failed to delete metadata files for directory: ${directory.id}",
                    LogSource.SERVICE,
                    throwable = exception,
                )
            }
        }

        companion object {
            private const val TAG = "MetadataCleaner"
        }
    }
