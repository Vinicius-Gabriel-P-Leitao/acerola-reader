package br.acerola.manga.dto

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.category.CategoryDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class MangaDto(
    val directory: MangaDirectoryDto,
    val remoteInfo: MangaRemoteInfoDto?,
    val category: CategoryDto? = null
): Parcelable