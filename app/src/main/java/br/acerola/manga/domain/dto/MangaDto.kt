package br.acerola.manga.domain.dto

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import br.acerola.manga.domain.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.domain.dto.archive.MangaFolderDto
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class MangaDto(
    val folder: MangaFolderDto,
    val metadata: MangaMetadataDto?,
): Parcelable