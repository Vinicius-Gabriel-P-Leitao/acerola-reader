package br.acerola.manga.local.database.entity.relation

import androidx.room.Embedded
import androidx.room.Relation
import br.acerola.manga.local.database.entity.metadata.MangaRemoteInfo
import br.acerola.manga.local.database.entity.metadata.relationship.Author
import br.acerola.manga.local.database.entity.metadata.relationship.Cover
import br.acerola.manga.local.database.entity.metadata.relationship.Genre

data class RemoteInfoRelations(
    @Embedded val remoteInfo: MangaRemoteInfo,

    @Relation(
        parentColumn = "manga_author_fk",
        entityColumn = "id"
    )
    val author: Author?,

    @Relation(
        parentColumn = "manga_cover_fk",
        entityColumn = "id"
    )
    val cover: Cover?,

    @Relation(
        parentColumn = "manga_genre_fk",
        entityColumn = "id"
    )
    val genre: Genre?

)