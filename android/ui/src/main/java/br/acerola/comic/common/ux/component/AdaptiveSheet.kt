package br.acerola.comic.common.ux.component
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.acerola.comic.common.ux.Acerola

import androidx.compose.foundation.shape.CornerSize
import br.acerola.comic.common.ux.tokens.ShapeTokens
import br.acerola.comic.common.ux.tokens.SizeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Acerola.Component.AdaptiveSheet(
    onDismissRequest: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) { onDismissRequest() },
                )
                Surface(
                    modifier =
                        Modifier
                            .width(SizeTokens.SideSheetWidth)
                            .fillMaxHeight()
                            .align(Alignment.CenterEnd),
                    color = containerColor,
                    contentColor = contentColor,
                    shape = ShapeTokens.Large.copy(topEnd = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        content()
                    }
                }
            }
        }
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = rememberModalBottomSheetState(),
            containerColor = containerColor,
            contentColor = contentColor,
        ) {
            content()
        }
    }
}
