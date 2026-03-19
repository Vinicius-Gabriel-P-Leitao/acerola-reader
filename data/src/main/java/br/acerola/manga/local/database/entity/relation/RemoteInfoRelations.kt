package br.acerola.manga.local.database.entity.relation

import androidx.room.Embedded
import androidx.room.Relation
import br.acerola.manga.local.database.entity.metadata.MangaRemoteInfo
import br.acerola.manga.local.database.entity.metadata.relationship.Author
import br.acerola.manga.local.database.entity.metadata.relationship.Banner
import br.acerola.manga.local.database.entity.metadata.relationship.Cover
import br.acerola.manga.local.database.entity.metadata.relationship.Genre
import br.acerola.manga.local.database.entity.metadata.source.AnilistSource
import br.acerola.manga.local.database.entity.metadata.source.ComicInfoSource
import br.acerola.manga.local.database.entity.metadata.source.MangadexSource

data class RemoteInfoRelations(
    @Embedded val remoteInfo: MangaRemoteInfo,

    @Relation(
        parentColumn = "id",
        entityColumn = "manga_remote_info_fk"
    )
    val mangadexSource: MangadexSource?,

    @Relation(
        parentColumn = "id",
        entityColumn = "manga_remote_info_fk"
    )
    val anilistSource: AnilistSource?,

    @Relation(
        parentColumn = "id",
        entityColumn = "manga_remote_info_fk"
    )
    val comicInfoSource: ComicInfoSource?,

    @Relation(
        parentColumn = "id",
        entityColumn = "manga_remote_info_fk"
    )
    val author: List<Author>,

    @Relation(
        parentColumn = "id",
        entityColumn = "manga_remote_info_fk"
    )
    val cover: List<Cover>,

    @Relation(
        parentColumn = "id",
        entityColumn = "manga_remote_info_fk"
    )
    val banner: List<Banner>,

    @Relation(
        parentColumn = "id",
        entityColumn = "manga_remote_info_fk"
    )
    val genre: List<Genre>
)
