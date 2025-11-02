package br.acerola.manga.domain.model

import androidx.room.Embedded
import androidx.room.Ignore
import br.acerola.manga.domain.model.archive.ChapterFile
import br.acerola.manga.domain.model.metadata.ChapterMetadata

data class ChapterFileWithMetadata(
    @Embedded(prefix = "file_")
    val file: ChapterFile,

    @Embedded(prefix = "metadata_")
    val metadata: ChapterMetadata,
)