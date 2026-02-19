package br.acerola.manga.service.metadata

import android.util.Xml
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.AuthorDto
import br.acerola.manga.dto.metadata.manga.GenreDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.local.database.entity.metadata.MetadataSource
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer
import java.io.InputStream
import java.io.StringWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComicInfoParserService @Inject constructor() {

    fun parseMangaInfo(inputStream: InputStream): MangaRemoteInfoDto {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        var title = ""
        var series = ""
        var summary = ""
        var writer = ""
        var genres = ""
        var year: Int? = null

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue

            when (parser.name) {
                "Title" -> title = readText(parser)
                "Series" -> series = readText(parser)
                "Summary" -> summary = readText(parser)
                "Writer" -> writer = readText(parser)
                "Genre" -> genres = readText(parser)
                "Year" -> year = readText(parser).toIntOrNull()
            }
        }

        val finalTitle = series.ifBlank { title }

        return MangaRemoteInfoDto(
            mirrorId = "local-${finalTitle.hashCode()}",
            title = finalTitle,
            description = summary,
            year = year,
            status = "Unknown",
            metadataSource = MetadataSource.COMIC_INFO,
            authors = if (writer.isNotBlank()) AuthorDto(id = "local-author", name = writer, type = "author") else null,
            genre = genres.split(",").mapNotNull {
                val g = it.trim()
                if (g.isNotBlank()) GenreDto(id = "local-$g", name = g) else null
            }
        )
    }

    fun parseChapterInfo(inputStream: InputStream): ChapterRemoteInfoDto {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        var title = ""
        var number = ""
        var volume = ""
        var pageCount = 0

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue

            when (parser.name) {
                "Title" -> title = readText(parser)
                "Number" -> number = readText(parser)
                "Volume" -> volume = readText(parser)
                "PageCount" -> pageCount = readText(parser).toIntOrNull() ?: 0
            }
        }

        return ChapterRemoteInfoDto(
            id = "local-$number",
            chapter = number,
            volume = volume,
            title = title,
            pages = pageCount,
            mangadexVersion = 0
        )
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
            parser.nextTag()
        }
        return result
    }
}
