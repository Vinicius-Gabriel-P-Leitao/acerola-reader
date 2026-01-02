package br.acerola.manga.module.manga.layout

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.manga.module.manga.MainTab

@Composable
fun MangaTabs(
    totalChapters: Int,
    activeTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    textColor: Color,
    secondaryTextColor: Color,
    primaryColor: Color
) {
    val tabs = MainTab.entries.toTypedArray()

    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 20.dp),
    ) {
        tabs.forEach { tab ->
            val isActive = tab == activeTab

            val title =
                tab.takeIf { it == MainTab.CHAPTERS }?.let { stringResource(id = it.titleRes, totalChapters) }
                    ?: stringResource(id = tab.titleRes)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(end = 24.dp)
                    .clickable { onTabSelected(tab) },
            ) {
                Text(
                    text = title,
                    color = if (isActive) textColor else secondaryTextColor,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    ),
                )

                if (isActive) {
                    Spacer(modifier = Modifier.height(height = 4.dp))
                    Box(
                        modifier = Modifier
                            .width(width = 20.dp)
                            .height(height = 3.dp)
                            .background(primaryColor, shape = RoundedCornerShape(size = 2.dp))
                    )
                }
            }
        }
    }
}