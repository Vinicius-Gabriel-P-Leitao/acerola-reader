package br.acerola.manga.service.reader.port

import java.io.InputStream

interface ChapterSourceService {

    suspend fun pageCount(): Int
    suspend fun openPage(index: Int): InputStream
}