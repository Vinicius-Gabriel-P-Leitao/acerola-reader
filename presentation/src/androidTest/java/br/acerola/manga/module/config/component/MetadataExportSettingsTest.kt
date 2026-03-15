package br.acerola.manga.module.config.component

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.acerola.manga.common.ux.theme.AcerolaTheme
import br.acerola.manga.module.main.Main
import br.acerola.manga.module.main.config.component.MetadataExportSettings
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
                    onCheckedChange = { checked = it }
                )
            }
        }

        // Verifica se o título e a descrição da configuração aparecem
        composeTestRule.onNodeWithText("Gerar ComicInfo.xml", substring = true).assertIsDisplayed()
        
        // Clica no switch usando matcher de Role
        composeTestRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .performClick()
        
        assert(!checked)
    }
}
