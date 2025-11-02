package br.acerola.manga.domain.database.converter

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString(separator = ",")

    @TypeConverter
    fun toStringList(data: String): List<String> = data.split(",")
}