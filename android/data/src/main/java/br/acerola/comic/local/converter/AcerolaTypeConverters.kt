package br.acerola.comic.local.converter

import androidx.room.TypeConverter
import br.acerola.comic.local.entity.metadata.relationship.TypeAuthor

class AcerolaTypeConverters {

    @TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString(separator = ",")

    @TypeConverter
    fun toStringList(data: String): List<String> = data.split(",")

    @TypeConverter
    fun toStatusChat(type: String): TypeAuthor = TypeAuthor.getByType(type)

    @TypeConverter
    fun fromStatusChat(status: TypeAuthor): String = status.type
}
