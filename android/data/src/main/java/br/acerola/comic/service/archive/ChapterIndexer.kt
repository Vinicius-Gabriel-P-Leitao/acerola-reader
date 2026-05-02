package br.acerola.comic.service.archive

import br.acerola.comic.local.entity.archive.ChapterArchive
import br.acerola.comic.local.translator.persistence.toChapterArchiveEntity
import br.acerola.comic.util.file.FastFileMetadata
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterIndexer
    @Inject
    constructor() {
        fun buildEntity(
            file: FastFileMetadata,
            comicId: Long,
            fileUri: String,
            chapterSort: String,
            fastHash: String,
            volumeIdFk: Long?,
            isSpecial: Boolean,
        ): ChapterArchive =
            file.toChapterArchiveEntity(
                comicId = comicId,
                fileUri = fileUri,
                chapterSort = chapterSort,
                fastHash = fastHash,
                volumeIdFk = volumeIdFk,
                isSpecial = isSpecial,
            )
    }
