package br.acerola.manga.local.converter

import androidx.room.TypeConverter
import br.acerola.manga.config.pattern.MetadataSource
import br.acerola.manga.local.database.entity.metadata.relationship.TypeAuthor

class DatabaseConverters {

    @TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString(separator = ",")

    @TypeConverter
    fun toStringList(data: String): List<String> = data.split(",")

    @TypeConverter
    fun toStatusChat(type: String): TypeAuthor = TypeAuthor.getByType(type)

    @TypeConverter
    fun fromStatusChat(status: TypeAuthor): String = status.type

    @TypeConverter
    fun toMetadataSource(source: String): MetadataSource = MetadataSource.from(source)

    @TypeConverter
    fun fromMetadataSource(source: MetadataSource): String = source.source
}