package br.acerola.comic.module.reader.coil
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import br.acerola.comic.service.reader.ReaderProcessor
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.Fetcher.Factory
import coil.request.Options

class PageFetcher(
    private val mangaId: Long,
    private val chapterId: Long,
    private val pageIndex: Int,
    private val readerProcessor: ReaderProcessor,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
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

class PageFetcherFactory(
    private val readerProcessor: ReaderProcessor,
) : Factory<Uri> {
    override fun create(
        data: Uri,
        options: Options,
        imageLoader: coil.ImageLoader,
    ): Fetcher? {
        if (data.scheme != "acerola" || data.host != "page") return null

        val segments = data.pathSegments
        if (segments.size < 3) return null

        val mangaId = segments[0].toLongOrNull() ?: return null
        val chapterId = segments[1].toLongOrNull() ?: return null
        val pageIndex = segments[2].toIntOrNull() ?: return null

        return PageFetcher(mangaId, chapterId, pageIndex, readerProcessor)
    }
}
