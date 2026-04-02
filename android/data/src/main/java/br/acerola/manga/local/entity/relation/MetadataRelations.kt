package br.acerola.manga.local.entity.relation

import androidx.room.Embedded
import androidx.room.Relation
import br.acerola.manga.local.entity.metadata.MangaMetadata
import br.acerola.manga.local.entity.metadata.relationship.Author
import br.acerola.manga.local.entity.metadata.relationship.Banner
import br.acerola.manga.local.entity.metadata.relationship.Cover
import br.acerola.manga.local.entity.metadata.relationship.Genre
import br.acerola.manga.local.entity.metadata.source.AnilistSource
import br.acerola.manga.local.entity.metadata.source.ComicInfoSource
import br.acerola.manga.local.entity.metadata.source.MangadexSource

data class MetadataRelations(
    @Embedded val remoteInfo: MangaMetadata,

    @Relation(
        parentColumn = "id",
        entityColumn = "manga_metadata_fk"
    )
    val mangadexSource: MangadexSource?,

    @Relation(
        parentColumn = "id",
        entityColumn = "manga_metadata_fk"
    )
    val anilistSource: AnilistSource?,

    @Relation(
        parentColumn = "id",
        entityColumn = "manga_metadata_fk"
    )
    val comicInfoSource: ComicInfoSource?,

    @Relation(
        parentColumn = "id",
        entityColumn = "manga_metadata_fk"
    )
    val author: List<Author>,

    @Relation(
        parentColumn = "id",
        entityColumn = "manga_metadata_fk"
    )
    val cover: List<Cover>,

    @Relation(
        parentColumn = "id",
        entityColumn = "manga_metadata_fk"
    )
    val banner: List<Banner>,

    @Relation(
        parentColumn = "id",
        entityColumn = "manga_metadata_fk"
    )
    val genre: List<Genre>
)
