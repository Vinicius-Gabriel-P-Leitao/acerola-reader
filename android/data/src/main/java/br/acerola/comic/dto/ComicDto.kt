package br.acerola.comic.dto

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.dto.metadata.category.CategoryDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class ComicDto(
    val category: CategoryDto? = null,
    val directory: ComicDirectoryDto,
    val remoteInfo: ComicMetadataDto?,
): Parcelable