package br.acerola.comic.module.comic.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.dto.archive.VolumeArchiveDto
import br.acerola.comic.dto.archive.VolumeChapterGroupDto
import br.acerola.comic.module.comic.Comic
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VolumeSectionHeaderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun group(
        id: Long = 1L,
        name: String = "Volume 1",
        volumeSort: String = "0001",
        isSpecial: Boolean = false,
        coverUri: String? = null,
        totalChapters: Int = 8,
        loadedCount: Int = 8,
    ) = VolumeChapterGroupDto(
        volume = VolumeArchiveDto(id = id, name = name, volumeSort = volumeSort, isSpecial = isSpecial, coverUri = coverUri),
        items = emptyList(),
        totalChapters = totalChapters,
        loadedCount = loadedCount,
        hasMore = false,
    )

    @Test
    fun `deve_exibir_titulo_e_resumo_de_capitulos_carregados`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Component.VolumeSectionHeader(
                    group = group(name = "Volume 1", loadedCount = 5, totalChapters = 8),
                    expanded = false,
                    onToggleExpanded = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Volume 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 de 8", substring = true).assertIsDisplayed()
    }

    @Test
    fun `deve_derivar_o_numero_do_volume_a_partir_do_volumeSort`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Component.VolumeSectionHeader(
                    group = group(name = "Volume 3", volumeSort = "0003"),
                    expanded = false,
                    onToggleExpanded = {},
                )
            }
        }

        composeTestRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun `deve_exibir_selo_especial_mesmo_quando_colapsado`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Component.VolumeSectionHeader(
                    group = group(name = "Extra - Artbook", volumeSort = "0000", isSpecial = true),
                    expanded = false,
                    onToggleExpanded = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Especial").assertIsDisplayed()
    }

    @Test
    fun `nao_deve_exibir_selo_especial_em_volume_normal`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Component.VolumeSectionHeader(
                    group = group(isSpecial = false),
                    expanded = false,
                    onToggleExpanded = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Especial").assertDoesNotExist()
    }

    @Test
    fun `clique_no_cabecalho_deve_chamar_onToggleExpanded`() {
        var toggled = false

        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Component.VolumeSectionHeader(
                    group = group(name = "Volume 1"),
                    expanded = false,
                    onToggleExpanded = { toggled = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Volume 1").performClick()

        assertTrue(toggled)
    }

    @Test
    fun `modo_capa_deve_exibir_titulo_e_resumo_de_capitulos_mesmo_sem_capa`() {
        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Component.CoverVolumeSectionHeader(
                    group = group(name = "Volume 1", coverUri = null, totalChapters = 4, loadedCount = 4),
                    expanded = false,
                    onToggleExpanded = {},
                    onExtractCover = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Volume 1").assertIsDisplayed()
    }

    @Test
    fun `modo_capa_pressionar_e_segurar_deve_chamar_onExtractCover`() {
        var extracted = false

        composeTestRule.setContent {
            AcerolaTheme {
                Comic.Component.CoverVolumeSectionHeader(
                    group = group(name = "Volume 1"),
                    expanded = false,
                    onToggleExpanded = {},
                    onExtractCover = { extracted = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Volume 1").performTouchInput { longClick() }

        assertTrue(extracted)
    }
}
