package br.acerola.comic.module.main.config.component

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.module.main.Main
import org.junit.Rule
import org.junit.Test

class MetadataExportSettingsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `MetadataExportSettings_deve_exibir_switch_de_geracao_de_ComicInfo`() {
        var checked = true
        composeTestRule.setContent {
            AcerolaTheme {
                Main.Config.Component.MetadataExportSettings(
                    enabled = checked,
                    onCheckedChange = { checked = it },
                )
            }
        }

        // Verifica se o título e a descrição da configuração aparecem
        composeTestRule.onNodeWithText("Gerar ComicInfo.xml", substring = true).assertIsDisplayed()

        // Clica no switch usando matcher de Role
        composeTestRule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .performClick()

        assert(!checked)
    }
}
