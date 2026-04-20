package br.acerola.comic.local.entity.relation

import androidx.room.Embedded
import androidx.room.Relation
import br.acerola.comic.local.entity.metadata.ComicMetadata
import br.acerola.comic.local.entity.metadata.relationship.Author
import br.acerola.comic.local.entity.metadata.relationship.Banner
import br.acerola.comic.local.entity.metadata.relationship.Cover
import br.acerola.comic.local.entity.metadata.relationship.Genre
import br.acerola.comic.local.entity.metadata.source.AnilistSource
import br.acerola.comic.local.entity.metadata.source.ComicInfoSource
import br.acerola.comic.local.entity.metadata.source.MangadexSource

data class MetadataRelations(
    @Embedded val remoteInfo: ComicMetadata,
    @Relation(
        parentColumn = "id",
        entityColumn = "comic_metadata_fk",
    )
    val mangadexSource: MangadexSource?,
    @Relation(
        parentColumn = "id",
        entityColumn = "comic_metadata_fk",
    )
    val anilistSource: AnilistSource?,
    @Relation(
        parentColumn = "id",
        entityColumn = "comic_metadata_fk",
    )
    val comicInfoSource: ComicInfoSource?,
    @Relation(
        parentColumn = "id",
        entityColumn = "comic_metadata_fk",
    )
    val author: List<Author>,
    @Relation(
        parentColumn = "id",
        entityColumn = "comic_metadata_fk",
    )
    val cover: List<Cover>,
    @Relation(
        parentColumn = "id",
        entityColumn = "comic_metadata_fk",
    )
    val banner: List<Banner>,
    @Relation(
        parentColumn = "id",
        entityColumn = "comic_metadata_fk",
    )
    val genre: List<Genre>,
)
