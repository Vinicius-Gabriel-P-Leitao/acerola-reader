package br.acerola.comic.module.comic.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import br.acerola.comic.config.preference.types.VolumeViewType
import br.acerola.comic.module.comic.Comic
import br.acerola.comic.ui.R
import org.junit.Rule
import org.junit.Test

class VolumeStylePreferenceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun deve_permitir_trocar_o_estilo_de_visualizacao_de_volumes() {
        var selectedMode = VolumeViewType.VOLUME

        composeTestRule.setContent {
            Comic.Component.VolumeStylePreference(
                selected = selectedMode,
                onSelect = { selectedMode = it },
            )
        }

        // Clica na opção "Capas" (label_volume_style_cover)
        val coverLabel = context.getString(R.string.label_volume_style_cover)
        composeTestRule.onNodeWithText(coverLabel).performClick()

        assert(selectedMode == VolumeViewType.COVER_VOLUME)
    }
}
