package br.acerola.manga.shared.dto.manga

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import br.acerola.manga.shared.dto.archive.MangaFolderDto
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto
import kotlinx.parcelize.Parcelize


@Parcelize
@Immutable
data class MangaDto(
    val folder: MangaFolderDto,
    val metadata: MangaMetadataDto?,
): Parcelable