package br.acerola.manga.local.converter

import br.acerola.manga.config.pattern.MetadataSource
import br.acerola.manga.local.database.entity.metadata.relationship.TypeAuthor
import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseConvertersTest {

    private val converter = DatabaseConverters()

    @Test
    fun list_converters_devem_serializar_e_deserializar_corretamente() {
        val list = listOf("action", "adventure", "comedy")
        val joined = converter.fromStringList(list)
        assertEquals("action,adventure,comedy", joined)

        val split = converter.toStringList(joined)
        assertEquals(list, split)
    }

    @Test
    fun metadata_source_converter_deve_ser_bidirecional() {
        val source = MetadataSource.COMIC_INFO
        val stringValue = converter.fromMetadataSource(source)
        assertEquals("comic_info", stringValue)

        val backToEnum = converter.toMetadataSource(stringValue)
        assertEquals(source, backToEnum)
    }

    @Test
    fun type_author_converter_deve_ser_bidirecional() {
        val type = TypeAuthor.ARTIST
        val stringValue = converter.fromStatusChat(type)
        assertEquals("artist", stringValue)

        val backToEnum = converter.toStatusChat(stringValue)
        assertEquals(type, backToEnum)
    }
}
