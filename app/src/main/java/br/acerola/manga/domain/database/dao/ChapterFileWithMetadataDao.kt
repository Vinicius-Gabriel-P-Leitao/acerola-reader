package br.acerola.manga.domain.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import br.acerola.manga.domain.model.ChapterFileWithMetadata

@Dao
interface ChapterDao {

    @Transaction
    @Query(
        """
        SELECT 
            cf.id AS file_id,
            cf.chapter AS file_chapter,
            cf.chapter_path AS file_chapter_path,
            cf.folder_path_fk AS file_folder_path_fk, 

            cm.id AS metadata_id,
            cm.chapter AS metadata_chapter,
            cm.title AS metadata_title,
            cm.release_date AS metadata_release_date,
            cm.summary AS metadata_summary,
            cm.page_count AS metadata_page_count,
            cm.scanlator AS metadata_scanlator,
            cm.read AS metadata_read,
            cm.manga_metadata_fk AS metadata_manga_metadata_fk 
        FROM
            chapter_file cf
        INNER JOIN
            chapter_metadata cm
        ON 
            cf.chapter = cm.chapter
        """
    )
    suspend fun getMangaWithMetadata(): List<ChapterFileWithMetadata>
}
