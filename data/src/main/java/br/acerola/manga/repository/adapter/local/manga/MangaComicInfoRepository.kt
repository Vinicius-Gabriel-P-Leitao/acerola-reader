package br.acerola.manga.repository.adapter.local.manga

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.repository.port.RemoteInfoOperationsRepository
import br.acerola.manga.service.metadata.ComicInfoParserService
import br.acerola.manga.service.reader.ChapterSourceFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaComicInfoRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val parser: ComicInfoParserService,
    private val chapterSourceFactory: ChapterSourceFactory
) : RemoteInfoOperationsRepository<MangaRemoteInfoDto, String> {

    override suspend fun searchInfo(
        manga: String,
        limit: Int,
        offset: Int,
        onProgress: ((Int) -> Unit)?,
        vararg extra: String?
    ): Either<NetworkError, List<MangaRemoteInfoDto>> = withContext(Dispatchers.IO) {
        val folderUri = extra.getOrNull(0)?.toUri()
            ?: return@withContext Either.Left(NetworkError.UnexpectedError(cause = Exception("Folder URI missing in extra[0]")))

        val folderDoc = DocumentFile.fromTreeUri(context, folderUri)
            ?: return@withContext Either.Left(NetworkError.NotFound())

        // 1. Tenta buscar na raiz
        val directXml = folderDoc.findFile("ComicInfo.xml")
        if (directXml != null && directXml.exists()) {
            return@withContext try {
                context.contentResolver.openInputStream(directXml.uri)?.use {
                    Either.Right(listOf(parser.parseMangaInfo(it)))
                } ?: Either.Left(NetworkError.NotFound())
            } catch (e: Exception) {
                Either.Left(NetworkError.UnexpectedError(cause = e))
            }
        }

        // 2. Tenta extrair do primeiro capítulo
        val firstChapter =
            folderDoc.listFiles().firstOrNull { it.isFile && (it.name?.endsWith(".cbz") == true || it.name?.endsWith(".cbr") == true) }
        if (firstChapter != null) {
            val chapterDto = ChapterFileDto(
                id = 0,
                name = firstChapter.name!!,
                path = firstChapter.uri.toString(),
                chapterSort = "0"
            )
            val sourceResult = chapterSourceFactory.create(chapterDto)

            return@withContext sourceResult.fold(
                ifLeft = { Either.Left(NetworkError.NotFound()) },
                ifRight = { source ->
                    source.getFileStream("ComicInfo.xml").fold(
                        ifLeft = { Either.Left(NetworkError.NotFound()) },
                        ifRight = { stream ->
                            try {
                                stream.use {
                                    Either.Right(listOf(parser.parseMangaInfo(it)))
                                }
                            } catch (e: Exception) {
                                Either.Left(NetworkError.UnexpectedError(cause = e))
                            }
                        }
                    )
                }
            )
        }

        Either.Left(NetworkError.NotFound())
    }

    override suspend fun saveInfo(manga: String, info: MangaRemoteInfoDto): Either<NetworkError, Unit> =
        withContext(Dispatchers.IO) {
            // NOTE: manga aqui deve ser o URI da pasta pai (root) e info.title o nome da subpasta
            // Mas por simplicidade, vamos assumir que manga é o URI da pasta do mangá
            val folderUri = manga.toUri()
            val folderDoc = DocumentFile.fromTreeUri(context, folderUri)
                ?: return@withContext Either.Left(NetworkError.NotFound())

            try {
                val xmlFile = folderDoc.findFile("ComicInfo.xml") ?: folderDoc.createFile("text/xml", "ComicInfo.xml")
                if (xmlFile != null) {
                    context.contentResolver.openOutputStream(xmlFile.uri)?.use { output ->
                        OutputStreamWriter(output).use { writer ->
                            writer.write(parser.serialize(info))
                        }
                    }
                    Either.Right(Unit)
                } else {
                    Either.Left(NetworkError.UnexpectedError(cause = Exception("Could not create ComicInfo.xml")))
                }
            } catch (e: Exception) {
                Either.Left(NetworkError.UnexpectedError(cause = e))
            }
        }
}