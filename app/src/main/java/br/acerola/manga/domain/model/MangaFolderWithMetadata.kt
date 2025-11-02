package br.acerola.manga.domain.model

import androidx.room.Embedded
import androidx.room.Ignore
import br.acerola.manga.domain.model.archive.MangaFolder
import br.acerola.manga.domain.model.metadata.MangaMetadata

data class MangaFolderWithMetadata(
    @Embedded(prefix = "folder_")
    val folder: MangaFolder,

    @Embedded(prefix = "metadata_")
    val metadata: MangaMetadata,
)