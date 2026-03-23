package br.acerola.manga.adapter.metadata.comicinfo.source

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.manga.adapter.contract.provider.MetadataProvider
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.service.metadata.ComicInfoParser
import br.acerola.manga.service.reader.ChapterSourceFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaComicInfoSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val parser: ComicInfoParser,
    private val chapterSourceFactory: ChapterSourceFactory
) : MetadataProvider<MangaMetadataDto, String> {

    override suspend fun searchInfo(
        manga: String,
        limit: Int,
        offset: Int,
        onProgress: ((Int) -> Unit)?,
        vararg extra: String?,
    ): Either<NetworkError, List<MangaMetadataDto>> = withContext(context = Dispatchers.IO) {
        val folderUri = extra.getOrNull(index = 0)?.toUri()
            ?: return@withContext Either.Left(value = NetworkError.UnexpectedError(cause = Exception("Folder URI missing in extra[0]")))

        val folderDoc = DocumentFile.fromTreeUri(context, folderUri)
        if (folderDoc == null || !folderDoc.exists()) {
            return@withContext Either.Left(value = NetworkError.NotFound())
        }

        // 1. Tenta encontrar o ComicInfo.xml direto na raiz da pasta
        val directXml = folderDoc.findFile("ComicInfo.xml")

        if (directXml != null && directXml.exists()) {
            return@withContext try {
                context.contentResolver.openInputStream(directXml.uri)?.use {
                    parser.parseMangaInfo(inputStream = it)
                        .map { info -> listOf(info) }
                        .mapLeft { error -> NetworkError.UnexpectedError(cause = Exception(error.toString())) }
                } ?: Either.Left(value = NetworkError.NotFound())
            } catch (exception: Exception) {
                Either.Left(value = NetworkError.UnexpectedError(cause = exception))
            }
        }

        // 2. Se não achou na raiz, procura dentro dos capítulos (cbz/cbr)
        val firstChapter = folderDoc.listFiles().firstOrNull {
            it.isFile && (it.name?.endsWith(suffix = ".cbz") == true || it.name?.endsWith(suffix = ".cbr") == true)
        }

        if (firstChapter != null) {
            val chapterDto = ChapterFileDto(
                id = 0,
                chapterSort = "0",
                name = firstChapter.name!!,
                path = firstChapter.uri.toString(),
            )
            val sourceResult = chapterSourceFactory.create(chapterDto)

            return@withContext sourceResult.fold(
                ifLeft = { Either.Left(value = NetworkError.NotFound()) },
                ifRight = { source ->
                    try {
                        source.getFileStream(fileName = "ComicInfo.xml").fold(
                            ifLeft = { Either.Left(value = NetworkError.NotFound()) },
                            ifRight = { stream ->
                                try {
                                    stream.use {
                                        parser.parseMangaInfo(inputStream = it)
                                            .map { info -> listOf(info) }
                                            .mapLeft { error -> NetworkError.UnexpectedError(cause = Exception(error.toString())) }
                                    }
                                } catch (exception: Exception) {
                                    Either.Left(value = NetworkError.UnexpectedError(cause = exception))
                                }
                            }
                        )
                    } finally {
                        source.close()
                    }
                }
            )
        }

        Either.Left(value = NetworkError.NotFound())
    }

    override suspend fun saveInfo(manga: String, info: MangaMetadataDto): Either<NetworkError, Unit> =
        withContext(context = Dispatchers.IO) {
            // NOTE: manga aqui deve ser o URI da pasta pai (root) e info.title o nome da subpasta
            // Mas por simplicidade, vamos assumir que manga é o URI da pasta do mangá
            val folderUri = manga.toUri()
            val folderDoc = DocumentFile.fromTreeUri(context, folderUri)
                ?: return@withContext Either.Left(value = NetworkError.NotFound())

            try {
                val xmlFile = folderDoc.findFile("ComicInfo.xml") ?: folderDoc.createFile("text/xml", "ComicInfo.xml")
                if (xmlFile != null) {
                    context.contentResolver.openOutputStream(xmlFile.uri)?.use { output ->
                        OutputStreamWriter(output).use { writer ->
                            writer.write(parser.serialize(info))
                        }
                    }
                    Either.Right(value = Unit)
                } else {
                    Either.Left(
                        value = NetworkError.UnexpectedError(
                            cause = Exception("Could not create ComicInfo .xml")
                        )
                    )
                }
            } catch (exception: Exception) {
                Either.Left(value = NetworkError.UnexpectedError(cause = exception))
            }
        }
}
