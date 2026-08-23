package br.acerola.comic.service.archive

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import arrow.core.getOrElse
import br.acerola.comic.local.dao.archive.VolumeArchiveDao
import br.acerola.comic.local.translator.persistence.toVolumeArchiveEntity
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.pattern.media.MediaFile
import br.acerola.comic.util.file.ContentQueryHelper
import br.acerola.comic.util.file.FastFileMetadata
import br.acerola.comic.util.file.toFastMetadata
import br.acerola.comic.util.sort.SortNormalizer
import br.acerola.comic.util.sort.SortType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VolumeSyncService
    @Inject
    constructor(
        private val volumeArchiveDao: VolumeArchiveDao,
        @param:ApplicationContext private val context: Context,
    ) {
        suspend fun sync(
            comicId: Long,
            subFolders: List<FastFileMetadata>,
            volumeTemplates: List<String>,
            baseUri: Uri?,
            folderUri: Uri,
        ): Map<String, Long> {
            val volumeMap = mutableMapOf<String, Long>()
            val existingVolumes = volumeArchiveDao.getVolumesListByDirectoryId(comicId)
            val existingVolumesMap = existingVolumes.associateBy { it.path }
            val volumesToDelete = existingVolumes.toMutableList()

            subFolders.forEach { subFolder ->
                val sortResult = SortNormalizer.normalize(subFolder.name, SortType.VOLUME, volumeTemplates)
                val subFolderUri =
                    if (baseUri != null) {
                        DocumentsContract.buildDocumentUriUsingTree(baseUri, subFolder.id).toString()
                    } else {
                        DocumentsContract.buildDocumentUriUsingTree(folderUri, subFolder.id).toString()
                    }

                val existing = existingVolumesMap[subFolderUri]
                if (existing != null) {
                    volumesToDelete.remove(existing)
                    volumeMap[subFolderUri] = existing.id
                    return@forEach
                }

                val subFolderChildren =
                    if (baseUri != null) {
                        ContentQueryHelper.listFiles(context, baseUri, subFolder.id).getOrElse { emptyList() }
                    } else {
                        DocumentFile.fromSingleUri(context, subFolderUri.toUri())?.listFiles()?.map { it.toFastMetadata() }
                            ?: emptyList()
                    }

                val cover =
                    subFolderChildren.find { MediaFile.isCover(it.name) }?.let {
                        if (baseUri != null) {
                            DocumentsContract.buildDocumentUriUsingTree(baseUri, it.id).toString()
                        } else {
                            DocumentsContract.buildDocumentUriUsingTree(subFolderUri.toUri(), it.id).toString()
                        }
                    }

                val banner =
                    subFolderChildren.find { MediaFile.isBanner(it.name) }?.let {
                        if (baseUri != null) {
                            DocumentsContract.buildDocumentUriUsingTree(baseUri, it.id).toString()
                        } else {
                            DocumentsContract.buildDocumentUriUsingTree(subFolderUri.toUri(), it.id).toString()
                        }
                    }

                val newVolume =
                    subFolder.toVolumeArchiveEntity(
                        comicId = comicId,
                        volumeSort = sortResult.normalizedSort,
                        folderUri = subFolderUri,
                        isSpecial = sortResult.isSpecial,
                        coverPath = cover,
                        bannerPath = banner,
                    )
                val newId = volumeArchiveDao.insert(newVolume)
                volumeMap[subFolderUri] = newId
            }

            if (volumesToDelete.isNotEmpty()) {
                AcerolaLogger.d(TAG, "Deleting ${volumesToDelete.size} stale volumes", LogSource.REPOSITORY)
                volumesToDelete.forEach { volumeArchiveDao.delete(it) }
            }

            return volumeMap
        }

        companion object {
            private const val TAG = "VolumeSyncService"
        }
    }
