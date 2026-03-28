package br.acerola.manga.dto

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.category.CategoryDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class MangaDto(
    val category: CategoryDto? = null,
    val directory: MangaDirectoryDto,
    val remoteInfo: MangaMetadataDto?,
): Parcelable