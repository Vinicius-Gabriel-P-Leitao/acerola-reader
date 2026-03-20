package br.acerola.manga.service.metadata

import android.util.Xml
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.AuthorDto
import br.acerola.manga.dto.metadata.manga.GenreDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.source.MangaSourcesDto
import br.acerola.manga.dto.metadata.manga.source.ComicInfoSourceDto
import br.acerola.manga.error.message.ComicInfoError
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer
import java.io.InputStream
import java.io.StringWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComicInfoParserService @Inject constructor() {

    fun parseMangaInfo(inputStream: InputStream): Either<ComicInfoError, MangaRemoteInfoDto> = Either.catch {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        var title = ""
        var writer = ""
        var genres = ""
        var series = ""
        var summary = ""
        var year: Int? = null

        // Avança até a primeira tag
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.name == "ComicInfo") {
                    // Entrou no nó raiz, processa os filhos
                    processComicInfo(parser) { tag, value ->
                        when (tag) {
                            "Title" -> title = value
                            "Series" -> series = value
                            "Summary" -> summary = value
                            "Writer" -> writer = value
                            "Genre" -> genres = value
                            "Year" -> year = value.toIntOrNull()
                        }
                    }
                    break
                } else {
                    return ComicInfoError.MissingRootElement.left()
                }
            }
            eventType = parser.next()
        }

        val finalTitle = series.ifBlank { title }
        if (finalTitle.isBlank()) return ComicInfoError.UnrecognizedMetadata("Unknown").left()

        ParsedMangaInfo(title = finalTitle, writer = writer, genres = genres, summary = summary, year = year).toDto().right()
    }.mapLeft { ComicInfoError.InvalidXmlFormat(it) }.flatMap { it }

    fun parseChapterInfo(inputStream: InputStream): Either<ComicInfoError, ChapterRemoteInfoDto> = Either.catch {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        var title = ""
        var number = ""
        var volume = ""
        var pageCount = 0

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.name == "ComicInfo") {
                    processComicInfo(parser) { tag, value ->
                        when (tag) {
                            "Title" -> title = value
                            "Number" -> number = value
                            "Volume" -> volume = value
                            "PageCount" -> pageCount = value.toIntOrNull() ?: 0
                        }
                    }
                    break
                } else {
                    return ComicInfoError.MissingRootElement.left()
                }
            }
            eventType = parser.next()
        }

        ParsedChapterInfo(title = title, number = number, volume = volume, pageCount = pageCount).toDto().right()
    }.mapLeft { ComicInfoError.InvalidXmlFormat(it) }.flatMap { it }

    private fun processComicInfo(parser: XmlPullParser, onTagFound: (String, String) -> Unit) {
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "ComicInfo") && eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val tagName = parser.name
                val text = readText(parser)
                onTagFound(tagName, text)
            }
            eventType = parser.next()
        }
    }

    fun serialize(info: MangaRemoteInfoDto): String {
        val serializer: XmlSerializer = Xml.newSerializer()
        val writer = StringWriter()
        serializer.setOutput(writer)
        serializer.startDocument("UTF-8", true)
        serializer.startTag("", "ComicInfo")

        tag(serializer, "Series", info.title)
        tag(serializer, "Summary", info.description)
        tag(serializer, "Writer", info.authors?.name ?: "")
        tag(serializer, "Year", info.year?.toString() ?: "")
        tag(serializer, "Genre", info.genre.joinToString(", ") { it.name })
        tag(serializer, "Manga", "Yes")

        serializer.endTag("", "ComicInfo")
        serializer.endDocument()
        return writer.toString()
    }

    private fun tag(serializer: XmlSerializer, name: String, text: String) {
        if (text.isBlank()) return
        serializer.startTag("", name)
        serializer.text(text)
        serializer.endTag("", name)
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag() // Move para o END_TAG
        }
        return result
    }
}

private data class ParsedMangaInfo(
    val title: String,
    val writer: String,
    val genres: String,
    val summary: String,
    val year: Int?
)

private fun ParsedMangaInfo.toDto(): MangaRemoteInfoDto = MangaRemoteInfoDto(
    title = title,
    description = summary,
    year = year,
    status = "Unknown",
    authors = if (writer.isNotBlank()) AuthorDto(id = "local-author", name = writer, type = "author") else null,
    genre = genres.split(",", ";").mapNotNull {
        val g = it.trim()
        if (g.isNotBlank()) GenreDto(id = "local-$g", name = g) else null
    },
    sources = MangaSourcesDto(
        comicInfo = ComicInfoSourceDto(
            localHash = "local-${title.hashCode()}"
        )
    )
)

private data class ParsedChapterInfo(
    val title: String,
    val number: String,
    val volume: String,
    val pageCount: Int
)

private fun ParsedChapterInfo.toDto(): ChapterRemoteInfoDto = ChapterRemoteInfoDto(
    id = "local-$number",
    chapter = number,
    volume = volume,
    title = title,
    pages = pageCount,
    mangadexVersion = 0
)
