package br.acerola.manga.domain.service.api


// TODO: Criar uma data Class para tipar os retornos em Sucess e Fail, mas não sei se é a melhor escolha já que os throws são pegos globalmente.
interface ApiPort {
    interface MetadataOperations<R, P> {
        // NOTE: O vararg não é obrigatório por padrão.
        suspend fun searchManga(title: String, limit: Int = 10, offset: Int = 0, vararg extra: P?): List<R>
    }

    interface ArchiveOperations<P> {
        suspend fun searchCover(search: String, vararg extra: P?): ByteArray
    }
}