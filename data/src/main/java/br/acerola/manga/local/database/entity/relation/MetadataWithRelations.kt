package br.acerola.manga.local.database.entity.relation

import androidx.room.Embedded
import androidx.room.Relation
import br.acerola.manga.local.database.entity.metadata.MangaMetadata
import br.acerola.manga.local.database.entity.metadata.relationship.Author
import br.acerola.manga.local.database.entity.metadata.relationship.Cover
import br.acerola.manga.local.database.entity.metadata.relationship.Gender

data class MetadataWithRelations(
    @Embedded val metadata: MangaMetadata,

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
        parentColumn = "manga_gender_fk",
        entityColumn = "id"
    )
    val gender: Gender?

)