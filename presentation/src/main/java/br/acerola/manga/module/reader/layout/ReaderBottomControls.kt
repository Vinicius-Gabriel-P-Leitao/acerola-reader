package br.acerola.manga.module.reader.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.manga.presentation.R

@Composable
fun ReaderBottomControls(
    pageCount: Int,
    currentPage: Int,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    enableNavigation: Boolean = true
) {
    Surface(
        shape = RoundedCornerShape(size = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        modifier = Modifier
            .padding(all = 16.dp)
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.label_reader_page_format, currentPage + 1, pageCount),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.weight(weight = 1f))

            if (enableNavigation) {
                FilledTonalIconButton(
                    onClick = onPrevClick,
                    enabled = currentPage > 0
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(id = R.string.description_icon_pagination_previous)
                    )
                }

                Button(
                    onClick = onNextClick,
                    enabled = currentPage < pageCount - 1
                ) {
                    Text(text = stringResource(id = R.string.label_reader_next_page))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        modifier = Modifier.padding(start = 8.dp),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}
