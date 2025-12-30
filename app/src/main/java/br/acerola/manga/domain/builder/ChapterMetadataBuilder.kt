package br.acerola.manga.domain.builder

import br.acerola.manga.shared.dto.mangadex.MetadataChapterDto
import br.acerola.manga.shared.dto.mangadex.MetadataChapterFileDto
import br.acerola.manga.shared.dto.metadata.ChapterMetadataDto

object ChapterMetadataBuilder {
    fun fromChapterData(
        metadataDto: MetadataChapterDto,
        fileDto: MetadataChapterFileDto? = null
    ): ChapterMetadataDto {
        val attributes = metadataDto.attributes
        val scanlatorName = metadataDto.scanlationGroups
            .firstNotNullOfOrNull { it.attributes?.name }

        val pagesUrls = if (fileDto != null && fileDto.chapter.isNotEmpty()) {
            val dataSaver = fileDto.chapter.first()
            val baseUrl = fileDto.baseUrl
            val hash = dataSaver.hash

            dataSaver.data.map { fileName ->
                "$baseUrl/data/$hash/$fileName"
            }
        } else {
            emptyList()
        }

        return ChapterMetadataDto(
            id = metadataDto.id,
            volume = attributes.volume,
            chapter = attributes.chapter,
            title = attributes.title,
            scanlator = scanlatorName,
            pages = attributes.pages,
            pageUrls = pagesUrls
        )
    }

    fun fromChapterDataList(dataList: List<MetadataChapterDto>): List<ChapterMetadataDto> =
        dataList.map { fromChapterData(metadataDto = it, fileDto = null) }
}