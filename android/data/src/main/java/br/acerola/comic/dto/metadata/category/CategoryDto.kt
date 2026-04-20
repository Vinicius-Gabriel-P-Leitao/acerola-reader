package br.acerola.comic.dto.metadata.category

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class CategoryDto(
    val id: Long = 0,
    val name: String,
    val color: Int,
) : Parcelable
