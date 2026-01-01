package br.acerola.manga.dto

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import br.acerola.manga.dto.archive.MangaFolderDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class MangaDto(
    val folder: MangaFolderDto,
    val metadata: MangaMetadataDto?,
): Parcelable