package br.acerola.comic.local.dao.metadata

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import br.acerola.comic.local.dao.BaseDao
import br.acerola.comic.local.dao.metadata.relationship.AuthorDao
import br.acerola.comic.local.dao.metadata.relationship.GenreDao
import br.acerola.comic.local.dao.metadata.source.AnilistSourceDao
import br.acerola.comic.local.dao.metadata.source.ComicInfoSourceDao
import br.acerola.comic.local.dao.metadata.source.MangadexSourceDao
import br.acerola.comic.local.entity.metadata.ComicMetadata
import br.acerola.comic.local.entity.metadata.relationship.Author
import br.acerola.comic.local.entity.metadata.relationship.Genre
import br.acerola.comic.local.entity.metadata.source.AnilistSource
import br.acerola.comic.local.entity.metadata.source.ComicInfoSource
import br.acerola.comic.local.entity.metadata.source.MangadexSource
import br.acerola.comic.local.entity.relation.MetadataRelations
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

@Dao
interface ComicMetadataDao : BaseDao<ComicMetadata> {
    @Query(value = "SELECT * FROM comic_metadata ORDER BY id ASC")
    fun observeAllComics(): Flow<List<ComicMetadata>>

    @Query(value = "SELECT * FROM comic_metadata WHERE title = :title")
    fun observeComicByTitle(title: String): Flow<ComicMetadata?>

    @Query(value = "SELECT * FROM comic_metadata WHERE id = :mangaId")
    fun observeComicById(mangaId: Long): Flow<ComicMetadata?>

    @Query(value = "SELECT * FROM comic_metadata WHERE comic_directory_fk = :directoryId")
    fun observeComicByDirectoryId(directoryId: Long): Flow<ComicMetadata?>

    @Transaction
    @Query(value = "SELECT * FROM comic_metadata WHERE id = :mangaId")
    fun observeComicWithRelationsById(mangaId: Long): Flow<MetadataRelations?>

    @Transaction
    @Query(value = "SELECT * FROM comic_metadata WHERE comic_directory_fk = :directoryId")
    fun observeComicWithRelationsByDirectoryId(directoryId: Long): Flow<MetadataRelations?>

    @Transaction
    @Query(value = "SELECT * FROM comic_metadata ORDER BY title ASC")
    fun observeAllComicsWithRelations(): Flow<List<MetadataRelations>>

    @Transaction
    suspend fun upsertComicWithRelationsTransaction(
        metadata: ComicMetadata,
        authors: List<Author>,
        genres: List<Genre>,
        mangadexSource: MangadexSource? = null,
        anilistSource: AnilistSource? = null,
        comicInfoSource: ComicInfoSource? = null,
        authorDao: AuthorDao,
        genreDao: GenreDao,
        mangadexDao: MangadexSourceDao? = null,
        anilistDao: AnilistSourceDao? = null,
        comicInfoDao: ComicInfoSourceDao? = null,
    ): Long {
        val existing = observeComicByDirectoryId(metadata.mangaDirectoryFk!!).firstOrNull()

        val mangaId =
            if (existing != null) {
                update(metadata.copy(id = existing.id))
                existing.id
            } else {
                insert(metadata)
            }

        if (mangaId != -1L) {
            authorDao.deleteByMetadataId(mangaId)
            genreDao.deleteByMetadataId(mangaId)

            authors.forEach { authorDao.insert(it.copy(mangaRemoteInfoFk = mangaId)) }
            genres.forEach { genreDao.insert(it.copy(mangaRemoteInfoFk = mangaId)) }

            mangadexSource?.let { mangadexDao?.insert(it.copy(mangaRemoteInfoFk = mangaId)) }
            anilistSource?.let { anilistDao?.insert(it.copy(mangaRemoteInfoFk = mangaId)) }
            comicInfoSource?.let { comicInfoDao?.insert(it.copy(mangaRemoteInfoFk = mangaId)) }
        }

        return mangaId
    }
}
