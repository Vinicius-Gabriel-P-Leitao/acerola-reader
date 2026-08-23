package br.acerola.comic.coil

import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import br.acerola.comic.service.reader.ReaderProcessor
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.Fetcher.Factory
import coil.request.Options

/**
 * Fetcher customizado do Coil para interceptar requisições de imagens locais do nosso leitor.
 *
 * **Por que isso existe?**
 * Nossas páginas de mangá/comic não são arquivos soltos no sistema de arquivos. Elas estão
 * embutidas dentro de arquivos compactados (ex: .cbz, .zip). O Coil nativamente não sabe como
 * extrair e carregar uma página específica de dentro de um ZIP.
 *
 * **Como resolvemos:**
 * Criamos este "Plugin" pro Coil. Sempre que a UI pedir uma imagem usando nosso esquema customizado
 * (ex: "acerola://page/comicId/chapterId/5"), o Coil delega o trabalho pesado de decodificação
 * para este Fetcher, que por sua vez chama o [ReaderProcessor].
 *
 * **Vantagens Arquiteturais:**
 * 1. **Isolamento da UI:** A UI (Módulo de UI / Jetpack Compose) não precisa saber como um arquivo .cbz
 *    é extraído. Ela apenas pede para desenhar uma URL usando o `AsyncImage` do Coil.
 * 2. **Prevenção de OOM (Out Of Memory):** Não carregamos o Bitmap inteiro na ViewModel (isso estouraria a RAM).
 *    Deixamos que o Coil gerencie o ciclo de vida, decodificação em background e descarte do Bitmap quando
 *    a página sair da tela.
 * 3. **Cache Inteligente:** Aproveitamos todo o sistema de cache de memória em múltiplos níveis nativo do Coil.
 *
 * @see PageFetcherFactory
 */
class PageFetcher(
    private val pageIndex: Int,
    private val readerProcessor: ReaderProcessor,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        // O ReaderProcessor vai no disco, abre o arquivo (ex: ZIP/CBZ), extrai os bytes
        // da página solicitada e decodifica para um Bitmap (com otimizações).
        val result = readerProcessor.loadPage(pageIndex)

        return result.fold(
            ifLeft = { null },
            ifRight = { bitmap ->
                DrawableResult(
                    drawable = BitmapDrawable(null, bitmap),
                    isSampled = false,
                    dataSource = DataSource.MEMORY,
                )
            },
        )
    }
}

/**
 * Factory responsável por registrar o [PageFetcher] no ImageLoader global do app.
 *
 * **Fluxo de execução:**
 * 1. A UI chama `AsyncImage(model = "acerola://page/1/1/5")`.
 * 2. O Coil identifica o scheme "acerola" e host "page" e cai nesta factory.
 * 3. A factory extrai o índice da página da URL (ex: 5) e instancia o [PageFetcher].
 */
class PageFetcherFactory(
    private val readerProcessor: ReaderProcessor,
) : Factory<Uri> {
    override fun create(
        data: Uri,
        options: Options,
        imageLoader: ImageLoader,
    ): Fetcher? {
        // Ignora requisições que não pertençam ao nosso leitor
        if (data.scheme != "acerola" || data.host != "page") return null

        val segments = data.pathSegments
        if (segments.size < 3) return null

        // Tenta extrair o índice da página a partir da URL
        val pageIndex = segments[2].toIntOrNull() ?: return null

        return PageFetcher(pageIndex, readerProcessor)
    }
}
