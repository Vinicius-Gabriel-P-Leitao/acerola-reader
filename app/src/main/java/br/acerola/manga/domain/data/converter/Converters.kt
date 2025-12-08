package br.acerola.manga.domain.data.converter

import androidx.room.TypeConverter
import br.acerola.manga.domain.model.metadata.author.TypeAuthor

class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString(separator = ",")

    @TypeConverter
    fun toStringList(data: String): List<String> = data.split(",")

    @TypeConverter
    fun toStatusChat(type: String): TypeAuthor = TypeAuthor.getByType(type)

    @TypeConverter
    fun fromStatusChat(status: TypeAuthor): String = status.type
}