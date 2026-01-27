package br.acerola.manga.service.reader.port

import java.io.InputStream

// TODO:  fun open(chapter: ChapterFileDto): ChapterSourceService trazer essa função para essa interface
interface ChapterSourceService {

    suspend fun pageCount(): Int
    suspend fun openPage(index: Int): InputStream
}