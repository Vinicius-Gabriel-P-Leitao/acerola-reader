package br.acerola.manga.service.metadata

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import arrow.core.right
import br.acerola.manga.config.preference.MetadataPreference
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.dao.metadata.MangaMetadataDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataExporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val parserService: ComicInfoParser,
    private val directoryDao: MangaDirectoryDao,
    private val remoteInfoDao: MangaMetadataDao,
) {

    suspend fun exportFull(
        directoryId: Long,
        mangaInfo: MangaMetadataDto
    ): Either<LibrarySyncError, Unit> = withContext(Dispatchers.IO) {
        val shouldGenerate = MetadataPreference.generateComicInfoFlow(context).first()
        if (!shouldGenerate) return@withContext Unit.right()

        exportMangaMetadata(directoryId, mangaInfo)
    }

    suspend fun exportMangaMetadata(
        directoryId: Long,
        remoteInfo: MangaMetadataDto
    ): Either<LibrarySyncError, Unit> = withContext(Dispatchers.IO) {
        Either.catch {
            val directory = directoryDao.getMangaDirectoryById(directoryId)
                ?: throw NoSuchElementException("Directory not found")

            val folderDoc = DocumentFile.fromTreeUri(context, directory.path.toUri())
                ?: throw IllegalStateException("Cannot access folder")

            if (folderDoc.exists() && folderDoc.canWrite()) {
                val xmlContent = parserService.serialize(remoteInfo)
                writeXmlToFolder(folderDoc, "ComicInfo.xml", xmlContent)

                val remoteInfoEntity = remoteInfoDao.getMangaByDirectoryId(directoryId).firstOrNull()
                if (remoteInfoEntity != null && !remoteInfoEntity.hasComicInfo) {
                    remoteInfoDao.update(remoteInfoEntity.copy(hasComicInfo = true))
                }
            }
            Unit
        }.mapLeft { handleException(it) }
    }

    private fun writeXmlToFolder(folderDoc: DocumentFile, fileName: String, content: String) {
        val xmlFile = folderDoc.findFile(fileName) ?: folderDoc.createFile("text/xml", fileName) ?: throw IOException(
            "Could not create metadata file: $fileName"
        )

        context.contentResolver.openOutputStream(xmlFile.uri)?.use { output ->
            output.write(content.toByteArray(Charsets.UTF_8))
        } ?: throw IOException("Could not open output stream for: $fileName")
    }

    private fun handleException(throwable: Throwable): LibrarySyncError = when (throwable) {
        is NoSuchElementException -> LibrarySyncError.MalformedLibrary(throwable)
        is IllegalStateException -> LibrarySyncError.FolderAccessDenied(throwable)
        is IOException -> LibrarySyncError.DiskIOFailure("Export", throwable)
        else -> LibrarySyncError.UnexpectedError(throwable)
    }
}
