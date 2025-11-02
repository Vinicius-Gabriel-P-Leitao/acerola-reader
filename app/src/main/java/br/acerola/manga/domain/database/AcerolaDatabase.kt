package br.acerola.manga.domain.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.acerola.manga.domain.database.converter.Converters
import br.acerola.manga.domain.database.dao.ChapterDao
import br.acerola.manga.domain.database.dao.MangaDao
import br.acerola.manga.domain.database.dao.archive.ChapterFileDao
import br.acerola.manga.domain.database.dao.archive.MangaFolderDao
import br.acerola.manga.domain.database.dao.metadata.ChapterMetadataDao
import br.acerola.manga.domain.database.dao.metadata.MangaMetadataDao
import br.acerola.manga.domain.model.ChapterFileWithMetadata
import br.acerola.manga.domain.model.MangaFolderWithMetadata
import br.acerola.manga.domain.model.archive.ChapterFile
import br.acerola.manga.domain.model.archive.MangaFolder
import br.acerola.manga.domain.model.metadata.ChapterMetadata
import br.acerola.manga.domain.model.metadata.MangaMetadata

@Database(
    entities = [MangaFolder::class, ChapterFile::class, MangaMetadata::class, ChapterMetadata::class],
    exportSchema = false,
    version = 1,
)
@TypeConverters(Converters::class)
abstract class AcerolaDatabase : RoomDatabase() {
    abstract fun chapterFileDao(): ChapterFileDao
    abstract fun chapterMetadataDao(): ChapterMetadataDao
    abstract fun mangaFolderDao(): MangaFolderDao
    abstract fun mangaMetadataDao(): MangaMetadataDao
    abstract fun chapterDao(): ChapterDao
    abstract fun mangaDao(): MangaDao

    companion object {
        @Volatile
        private var INSTANCE: AcerolaDatabase? = null

        fun getInstance(context: Context): AcerolaDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext, klass = AcerolaDatabase::class.java, name = "acerola_database"
            ).build().also { INSTANCE = it }
        }
    }
}