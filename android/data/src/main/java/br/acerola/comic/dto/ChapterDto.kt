package br.acerola.comic.dto

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import br.acerola.comic.config.preference.types.VolumeViewType
import br.acerola.comic.dto.archive.ChapterPageDto
import br.acerola.comic.dto.metadata.chapter.ChapterRemoteInfoPageDto
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class ChapterDto(
    val archive: ChapterPageDto,
    val remoteInfo: ChapterRemoteInfoPageDto?,
    val showVolumeHeaders: Boolean = false,
    val hasVolumeStructure: Boolean = false,
    val effectiveViewMode: VolumeViewType = VolumeViewType.CHAPTER,
) : Parcelable
