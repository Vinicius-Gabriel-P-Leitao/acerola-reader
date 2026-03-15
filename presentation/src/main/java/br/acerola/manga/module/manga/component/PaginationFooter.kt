package br.acerola.manga.module.manga.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.manga.module.manga.Manga
import br.acerola.manga.presentation.R

@Composable
fun Manga.Component.PaginationFooter(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit
) {
    if (totalPages <= 1) return

    val lastPageIndex = totalPages - 1
    val currentBlock = currentPage / 5

    val startPage = currentBlock * 5
    val endPage = (startPage + 5).coerceAtMost(maximumValue = totalPages)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.label_pagination_format, currentPage + 1, totalPages),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(height = 8.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { onPageChange(currentPage - 1) }, enabled = currentPage > 0
            ) {
                Icon(
                    contentDescription = stringResource(id = R.string.description_icon_pagination_previous),
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    tint = if (currentPage > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.3f
                    )
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (page in startPage until endPage) {
                    val isSelected = page == currentPage

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(size = 36.dp)
                            .clip(shape = RoundedCornerShape(size = 8.dp))
                            .background(color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onPageChange(page) })
                    {
                        Text(
                            text = (page + 1).toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            IconButton(
                onClick = { onPageChange(currentPage + 1) },
                enabled = currentPage < lastPageIndex
            ) {
                Icon(
                    contentDescription = stringResource(id = R.string.description_icon_pagination_next),
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    tint = if (currentPage < lastPageIndex) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}
