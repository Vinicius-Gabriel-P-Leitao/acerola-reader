package br.acerola.manga.module.manga.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PaginationFooter(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit
) {
    val lastPageIndex = totalPages - 1

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth()
    ) {
        TextButton (
            onClick = { onPageChange(currentPage - 1) },
            enabled = currentPage > 0
        ) {
            Text(
                "Anterior",
                color = if (currentPage > 0) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
        }

        Text(
            text = "Página ${currentPage + 1} de $totalPages",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        TextButton(
            onClick = { onPageChange(currentPage + 1) },
            enabled = currentPage < lastPageIndex
        ) {
            Text(
                "Próximo",
                color = if (currentPage < lastPageIndex) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
        }
    }
}