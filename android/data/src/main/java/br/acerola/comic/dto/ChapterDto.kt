package br.acerola.comic.dto

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import br.acerola.comic.dto.archive.ChapterArchivePageDto
import br.acerola.comic.dto.metadata.chapter.ChapterRemoteInfoPageDto
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class ChapterDto(
    val archive: ChapterArchivePageDto,
    val remoteInfo: ChapterRemoteInfoPageDto?,
) : Parcelable
