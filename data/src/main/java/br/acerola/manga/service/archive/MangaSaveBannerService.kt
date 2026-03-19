package br.acerola.manga.service.archive

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.config.pattern.MediaFilePattern
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.metadata.relationship.BannerDao
import br.acerola.manga.local.database.entity.metadata.relationship.Banner
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaSaveBannerService @Inject constructor(
    private val bannerDao: BannerDao,
    private val directoryDao: MangaDirectoryDao,
    @param:ApplicationContext private val context: Context,
) {
    suspend fun processBanner(
        rootUri: Uri,
        folderId: Long,
        bytes: ByteArray,
        bannerUrl: String,
        mangaFolderName: String,
        mangaRemoteInfoFk: Long
    ): Long {
        var savedUriString: String? = null

        try {
            val rootDir = DocumentFile.fromTreeUri(context, rootUri)

            if (rootDir != null && rootDir.exists()) {
                var mangaDir = rootDir.findFile(mangaFolderName)

                if (mangaDir == null) {
                    mangaDir = rootDir.createDirectory(mangaFolderName)
                }

                if (mangaDir != null && mangaDir.canWrite()) {
                    val finalFileName = MediaFilePattern.BANNER.defaultFileName

                    val oldFile = mangaDir.findFile(finalFileName)
                    if (oldFile != null && oldFile.exists()) {
                        oldFile.delete()
                    }

                    val newFile = mangaDir.createFile("image/png", finalFileName)

                    if (newFile != null) {
                        context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                            outputStream.write(bytes)
                        }
                        savedUriString = newFile.uri.toString()
                    }
                }
            }
        } catch (exception: Exception) {
            println(exception)
        }

        if (savedUriString != null) {
            val directory = directoryDao.getMangaDirectoryById(mangaId = folderId)
            if (directory != null) {
                directoryDao.update(entity = directory.copy(banner = savedUriString))
            }
        }

        // TODO: Fazer toModel
        val bannerEntity = Banner(
            fileName = MediaFilePattern.BANNER.defaultFileName,
            url = bannerUrl,
            mangaRemoteInfoFk = mangaRemoteInfoFk
        )

        val insertedId = bannerDao.insert(entity = bannerEntity)

        return if (insertedId != -1L) {
            insertedId
        } else {
            val existing = bannerDao.getBannerByFileNameAndFk(
                fileName = MediaFilePattern.BANNER.defaultFileName,
                mangaRemoteInfoFk = mangaRemoteInfoFk
            ) ?: throw IllegalStateException("Banner not found for fk: $mangaRemoteInfoFk")

            bannerDao.update(entity = existing.copy(url = bannerUrl, fileName = MediaFilePattern.BANNER.defaultFileName))
            existing.id
        }
    }
}
