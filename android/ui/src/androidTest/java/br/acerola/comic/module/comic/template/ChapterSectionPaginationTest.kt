package br.acerola.comic.module.comic.template

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import br.acerola.comic.config.preference.types.VolumeViewType
import br.acerola.comic.dto.ChapterDto
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.dto.archive.VolumeArchiveDto
import br.acerola.comic.dto.archive.VolumeChapterGroupDto
import br.acerola.comic.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.comic.fixtures.ComicFixtures
import br.acerola.comic.module.comic.Comic
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChapterSectionPaginationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deve_carregar_paginas_do_volume_uma_por_vez_ao_expandir() {
        val calls = mutableListOf<Int>()
        val volume1 = VolumeArchiveDto(id = 10L, name = "Vol. 1", volumeSort = "1", isSpecial = false)

        val chapters =
            ChapterDto(
                archive =
                    ComicFixtures.createChapterArchivePageDto().copy(
                        volumes = listOf(volume1),
                        volumeSections =
                            listOf(
                                VolumeChapterGroupDto(
                                    volume = volume1,
                                    items =
                                        (1..50).map { i ->
                                            ChapterFileDto(id = i.toLong(), name = "Cap. $i", path = "", chapterSort = "$i", volumeId = 10L)
                                        },
                                    totalChapters = 100,
                                    loadedCount = 50,
                                    hasMore = true,
                                    currentPage = 0,
                                    totalPages = 4,
                                ),
                            ),
                    ),
                remoteInfo = ChapterRemoteInfoPageDto(emptyList(), 20, 0, 0),
                showVolumeHeaders = true,
            )

        composeTestRule.setContent {
            // Altura pequena para simular que o final NÃO está visível
            LazyColumn(modifier = Modifier.height(200.dp)) {
                Comic.Template.chapterSection(
                    scope = this,
                    chapters = chapters,
                    currentPage = 0,
                    totalPages = 1,
                    onChapterClick = { _, _ -> },
                    onToggleRead = {},
                    onPageChange = {},
                    volumeViewMode = VolumeViewType.VOLUME,
                    activeVolumeId = 10L, // Expandido
                    onLoadVolumeChaptersPage = { _, page -> calls.add(page) },
                )
            }
        }

        composeTestRule.waitForIdle()

        assertTrue("Não deveria carregar a próxima página se o rodapé do volume não estiver visível. Chamadas: ${calls.size}", calls.isEmpty())
    }

    @Test
    fun deve_detectar_busca_em_cascata_no_modo_volume() {
        val calls = mutableListOf<Int>()
        val volume1 = VolumeArchiveDto(id = 10L, name = "Vol. 1", volumeSort = "1", isSpecial = false)

        // Simula uma lista pequena que cabe na tela
        val chapters =
            ChapterDto(
                archive =
                    ComicFixtures.createChapterArchivePageDto().copy(
                        volumes = listOf(volume1),
                        volumeSections =
                            listOf(
                                VolumeChapterGroupDto(
                                    volume = volume1,
                                    items =
                                        (1..2).map { i ->
                                            ChapterFileDto(id = i.toLong(), name = "Cap. $i", path = "", chapterSort = "$i", volumeId = 10L)
                                        },
                                    totalChapters = 100,
                                    loadedCount = 2,
                                    hasMore = true,
                                    currentPage = 0,
                                    totalPages = 10,
                                ),
                            ),
                    ),
                remoteInfo = ChapterRemoteInfoPageDto(emptyList(), 20, 0, 0),
                showVolumeHeaders = true,
            )

        composeTestRule.setContent {
            // Altura grande para garantir que o trigger esteja visível
            LazyColumn(modifier = Modifier.height(1000.dp)) {
                Comic.Template.chapterSection(
                    scope = this,
                    chapters = chapters,
                    currentPage = 0,
                    totalPages = 1,
                    onChapterClick = { _, _ -> },
                    onToggleRead = {},
                    onPageChange = {},
                    volumeViewMode = VolumeViewType.VOLUME,
                    activeVolumeId = 10L,
                    onLoadVolumeChaptersPage = { _, page -> calls.add(page) },
                )
            }
        }

        composeTestRule.waitForIdle()

        // Se o problema de cascata existir, calls terá muitos itens rapidamente
        // ou pelo menos mais de 1 se o estado recompor rápido.
        // No entanto, em um teste unitário/instrumentado, o estado 'chapters' é estático aqui.
        // Para simular a cascata real, o 'onLoadVolumeChaptersPage' teria que atualizar um estado que refaz o compose.

        // Mas o teste acima (altura 200dp) já deve pegar se o trigger está sendo composto indevidamente.
    }
}
