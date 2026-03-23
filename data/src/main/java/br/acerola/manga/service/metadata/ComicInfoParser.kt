package br.acerola.manga.service.metadata

import android.util.Xml
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import br.acerola.manga.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.error.message.ComicInfoError
import br.acerola.manga.local.translator.infra.ParsedComicInfo
import br.acerola.manga.local.translator.infra.toChapterDto
import br.acerola.manga.local.translator.infra.toMangaDto
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer
import java.io.InputStream
import java.io.StringWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComicInfoParser @Inject constructor() {

    fun parseMangaInfo(inputStream: InputStream): Either<ComicInfoError, MangaMetadataDto> = Either.catch {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        var title = ""
        var writer = ""
        var genres = ""
        var series = ""
        var summary = ""
        var year: Int? = null

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.name == "ComicInfo") {
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

        if (series.isBlank() && title.isBlank()) return ComicInfoError.UnrecognizedMetadata("Unknown").left()

        ParsedComicInfo(
            title = title,
            series = series,
            writer = writer,
            genres = genres,
            summary = summary,
            year = year
        ).toMangaDto().right()
    }.mapLeft { ComicInfoError.InvalidXmlFormat(it) }.flatMap { it }

    fun parseChapterInfo(inputStream: InputStream): Either<ComicInfoError, ChapterMetadataDto> = Either.catch {
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

        ParsedComicInfo(
            title = title,
            number = number,
            volume = volume,
            pageCount = pageCount
        ).toChapterDto().right()
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

    fun serialize(info: MangaMetadataDto): String {
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
            parser.nextTag() 
        }
        return result
    }
}
