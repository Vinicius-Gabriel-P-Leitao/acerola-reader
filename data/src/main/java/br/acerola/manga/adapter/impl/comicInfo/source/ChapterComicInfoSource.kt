package br.acerola.manga.adapter.impl.comicInfo.source

import android.content.Context
import arrow.core.Either
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.adapter.port.RemoteInfoOperationsPort
import br.acerola.manga.service.metadata.ComicInfoParserService
import br.acerola.manga.service.reader.ChapterSourceFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterComicInfoSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val parser: ComicInfoParserService,
    private val chapterSourceFactory: ChapterSourceFactory
) : RemoteInfoOperationsPort<ChapterRemoteInfoDto, String> {

    override suspend fun searchInfo(
        manga: String,
        limit: Int,
        offset: Int,
        onProgress: ((Int) -> Unit)?,
        vararg extra: String?
    ): Either<NetworkError, List<ChapterRemoteInfoDto>> = withContext(context = Dispatchers.IO) {
        // manga aqui é o URI do arquivo do capítulo
        val chapterUri = manga
        val chapterDto = ChapterFileDto(id = 0, name = "Unknown", path = chapterUri, chapterSort = "0")

        val sourceResult = chapterSourceFactory.create(chapterDto)

        sourceResult.fold(
            ifLeft = { Either.Left(value = NetworkError.NotFound()) },
            ifRight = { source ->
                try {
                    source.getFileStream(fileName = "ComicInfo.xml").fold(
                        ifLeft = { Either.Left(value = NetworkError.NotFound()) },
                        ifRight = { stream ->
                            try {
                                stream.use {
                                    parser.parseChapterInfo(it)
                                        .map { info -> listOf(info) }
                                        .mapLeft { error -> NetworkError.UnexpectedError(cause = Exception(error.toString())) }
                                }
                            } catch (e: Exception) {
                                Either.Left(value = NetworkError.UnexpectedError(cause = e))
                            }
                        }
                    )
                } finally {
                    source.close()
                }
            }
        )
    }

    override suspend fun saveInfo(manga: String, info: ChapterRemoteInfoDto): Either<NetworkError, Unit> {
        // NOTE: Atualmente não suportamos escrita dentro de CBZ/CBR (re-zipar)
        return Either.Right(value = Unit)
    }
}