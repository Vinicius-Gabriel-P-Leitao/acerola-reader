package br.acerola.manga.ui.feature.main.home.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.acerola.manga.R
import br.acerola.manga.ui.common.component.ButtonType
import br.acerola.manga.ui.common.component.SmartButton

@Composable
fun FilterButton() {
    val context = LocalContext.current
    SmartButton(type = ButtonType.ICON, modifier = Modifier.size(size = 48.dp), onClick = {
        println("Filtrar")
    }) {
        Icon(
            imageVector = Icons.Filled.FilterList,
            contentDescription = context.getString(R.string.description_icon_filter_mangas_catalog)
        )
    }
}
