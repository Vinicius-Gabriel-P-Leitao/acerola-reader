package br.acerola.comic.adapter.metadata.comicinfo.source

import android.content.Context
import arrow.core.Either
import br.acerola.comic.adapter.contract.provider.MetadataProvider
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.comic.error.message.NetworkError
import br.acerola.comic.service.metadata.ComicInfoParser
import br.acerola.comic.service.reader.ChapterSourceFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterComicInfoSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val parser: ComicInfoParser,
    private val chapterSourceFactory: ChapterSourceFactory
) : MetadataProvider<ChapterMetadataDto, String> {

    override suspend fun searchInfo(
        manga: String,
        limit: Int,
        offset: Int,
        onProgress: ((Int) -> Unit)?,
        vararg extra: String?
    ): Either<NetworkError, List<ChapterMetadataDto>> = withContext(context = Dispatchers.IO) {
        // comic aqui é o URI do arquivo do capítulo
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

    override suspend fun saveInfo(manga: String, info: ChapterMetadataDto): Either<NetworkError, Unit> {
        // NOTE: Atualmente não suportamos escrita dentro de CBZ/CBR (re-zipar)
        return Either.Right(value = Unit)
    }
}
