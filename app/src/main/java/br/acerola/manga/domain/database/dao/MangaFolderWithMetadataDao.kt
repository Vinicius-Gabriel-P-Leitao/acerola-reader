package br.acerola.manga.domain.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import br.acerola.manga.domain.model.MangaFolderWithMetadata

@Dao
interface MangaDao {

    @Transaction
    @Query(
        value = """
        SELECT 
            mf.id AS folder_id,
            mf.name AS folder_name,
            mf.path AS folder_path,
            mf.cover AS folder_cover,
            mf.banner AS folder_banner,
            mf.last_modified AS folder_last_modified,

            mm.id AS metadata_id,
            mm.name AS metadata_name,
            mm.description AS metadata_description,
            mm.romanji AS metadata_romanji,
            mm.gender AS metadata_gender,
            mm.publication AS metadata_publication,
            mm.author AS metadata_author
        FROM 
            manga_folder mf
        INNER JOIN 
            manga_metadata mm
        ON mf.name = mm.name
        """
    )
    suspend fun getMangaWithMetadata(): List<MangaFolderWithMetadata>
}
