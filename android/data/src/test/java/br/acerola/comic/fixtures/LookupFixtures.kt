package br.acerola.comic.fixtures

import br.acerola.comic.dto.metadata.comic.AuthorDto
import br.acerola.comic.dto.metadata.comic.CoverDto
import br.acerola.comic.dto.metadata.comic.GenreDto

object LookupFixtures {

    fun createAuthorDto(
        id: String = "auth-123",
        name: String = "Eiichiro Oda",
        type: String = "author"
    ) = AuthorDto(id = id, name = name, type = type)

    fun createGenreDto(
        id: String = "genre-123",
        name: String = "Action"
    ) = GenreDto(id = id, name = name)

    fun createCoverDto(
        id: String = "cover-123",
        url: String = "https://mangadex.org/covers/1/a.jpg",
        fileName: String = "a.jpg"
    ) = CoverDto(id = id, url = url, fileName = fileName)
}
