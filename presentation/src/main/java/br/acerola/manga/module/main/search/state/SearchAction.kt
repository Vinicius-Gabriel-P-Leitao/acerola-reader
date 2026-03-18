package br.acerola.manga.module.main.search.state

sealed interface SearchAction {
    data class QueryChanged(val query: String) : SearchAction
    object Search : SearchAction
}
