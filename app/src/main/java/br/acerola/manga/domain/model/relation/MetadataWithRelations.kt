package br.acerola.manga.domain.model.relation

import androidx.room.Embedded
import androidx.room.Relation
import br.acerola.manga.domain.model.metadata.MangaMetadata
import br.acerola.manga.domain.model.metadata.author.Author
import br.acerola.manga.domain.model.metadata.cover.Cover
import br.acerola.manga.domain.model.metadata.gender.Gender

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