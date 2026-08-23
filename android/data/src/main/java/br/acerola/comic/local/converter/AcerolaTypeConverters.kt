package br.acerola.comic.local.converter

import androidx.room.TypeConverter
import br.acerola.comic.local.entity.metadata.relationship.TypeAuthor
import br.acerola.comic.util.sort.SortType

class AcerolaTypeConverters {
    @TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString(separator = ",")

    @TypeConverter
    fun toStringList(data: String): List<String> = data.split(",")

    @TypeConverter
    fun toStatusChat(type: String): TypeAuthor = TypeAuthor.getByType(type)

    @TypeConverter
    fun fromStatusChat(status: TypeAuthor): String = status.type

    @TypeConverter
    fun toSortType(type: String): SortType = SortType.valueOf(type)

    @TypeConverter
    fun fromSortType(type: SortType): String = type.name
}
