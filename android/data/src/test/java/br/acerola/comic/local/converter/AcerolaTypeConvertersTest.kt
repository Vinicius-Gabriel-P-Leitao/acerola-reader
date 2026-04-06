package br.acerola.comic.local.converter

import br.acerola.comic.local.entity.metadata.relationship.TypeAuthor
import org.junit.Assert.assertEquals
import org.junit.Test

class AcerolaTypeConvertersTest {

    private val converter = AcerolaTypeConverters()

    @Test
    fun list_converters_devem_serializar_e_deserializar_corretamente() {
        val list = listOf("action", "adventure", "comedy")
        val joined = converter.fromStringList(list)
        assertEquals("action,adventure,comedy", joined)

        val split = converter.toStringList(joined)
        assertEquals(list, split)
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
