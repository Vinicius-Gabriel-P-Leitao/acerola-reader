package br.acerola.manga.local.dao.metadata

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import br.acerola.manga.local.dao.BaseDao
import br.acerola.manga.local.dao.metadata.relationship.AuthorDao
import br.acerola.manga.local.dao.metadata.relationship.GenreDao
import br.acerola.manga.local.dao.metadata.source.AnilistSourceDao
import br.acerola.manga.local.dao.metadata.source.ComicInfoSourceDao
import br.acerola.manga.local.dao.metadata.source.MangadexSourceDao
import br.acerola.manga.local.entity.metadata.MangaMetadata
import br.acerola.manga.local.entity.metadata.relationship.Author
import br.acerola.manga.local.entity.metadata.relationship.Genre
import br.acerola.manga.local.entity.metadata.source.AnilistSource
import br.acerola.manga.local.entity.metadata.source.ComicInfoSource
import br.acerola.manga.local.entity.metadata.source.MangadexSource
import br.acerola.manga.local.entity.relation.MetadataRelations
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

@Dao
interface MangaMetadataDao : BaseDao<MangaMetadata> {

    @Query(value = "SELECT * FROM manga_metadata ORDER BY id ASC")
    fun getAllMangaRemoteInfo(): Flow<List<MangaMetadata>>

    @Query(value = "SELECT * FROM manga_metadata WHERE title = :title")
    fun getMangaRemoteInfoByName(title: String): Flow<MangaMetadata?>

    @Query(value = "SELECT * FROM manga_metadata WHERE id = :mangaId")
    fun getMangaById(mangaId: Long): Flow<MangaMetadata?>

    @Query(value = "SELECT * FROM manga_metadata WHERE manga_directory_fk = :directoryId")
    fun getMangaByDirectoryId(directoryId: Long): Flow<MangaMetadata?>

    @Transaction
    @Query(value = "SELECT * FROM manga_metadata WHERE id = :mangaId")
    fun getMangaWithRelationsById(mangaId: Long): Flow<MetadataRelations?>

    @Transaction
    @Query(value = "SELECT * FROM manga_metadata WHERE manga_directory_fk = :directoryId")
    fun getMangaWithRelationsByDirectoryId(directoryId: Long): Flow<MetadataRelations?>

    @Transaction
    @Query(value = "SELECT * FROM manga_metadata ORDER BY title ASC")
    fun getAllMangasWithRelations(): Flow<List<MetadataRelations>>

    @Transaction
    suspend fun upsertMangaMetadataTransaction(
        metadata: MangaMetadata,
        authors: List<Author>,
        genres: List<Genre>,
        mangadexSource: MangadexSource? = null,
        anilistSource: AnilistSource? = null,
        comicInfoSource: ComicInfoSource? = null,
        authorDao: AuthorDao,
        genreDao: GenreDao,
        mangadexDao: MangadexSourceDao? = null,
        anilistDao: AnilistSourceDao? = null,
        comicInfoDao: ComicInfoSourceDao? = null
    ): Long {
        val existing = getMangaByDirectoryId(metadata.mangaDirectoryFk!!).firstOrNull()

        val mangaId = if (existing != null) {
            update(metadata.copy(id = existing.id))
            existing.id
        } else {
            insert(metadata)
        }

        if (mangaId != -1L) {
            authorDao.deleteAuthorsByMangaRemoteInfoFk(mangaId)
            genreDao.deleteGenresByMangaRemoteInfoFk(mangaId)

            authors.forEach { authorDao.insert(it.copy(mangaRemoteInfoFk = mangaId)) }
            genres.forEach { genreDao.insert(it.copy(mangaRemoteInfoFk = mangaId)) }

            mangadexSource?.let { mangadexDao?.insert(it.copy(mangaRemoteInfoFk = mangaId)) }
            anilistSource?.let { anilistDao?.insert(it.copy(mangaRemoteInfoFk = mangaId)) }
            comicInfoSource?.let { comicInfoDao?.insert(it.copy(mangaRemoteInfoFk = mangaId)) }
        }

        return mangaId
    }
}
