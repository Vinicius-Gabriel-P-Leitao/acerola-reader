package br.acerola.manga.service.reader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.error.message.ChapterError
import br.acerola.manga.service.cache.BitmapCacheService
import br.acerola.manga.service.reader.port.ChapterSourceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterReaderService @Inject constructor(
    private val factory: ChapterSourceFactory,
    private val bitmapCache: BitmapCacheService
) {

    private val tempStorage = ByteArray(64 * 1024)
    private lateinit var source: ChapterSourceService
    private val prefetchSemaphore = Semaphore(permits = 1)
    private val decodeDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    fun openChapter(chapter: ChapterFileDto): Either<ChapterError, Unit> {
        return factory.create(chapter).map { newSource ->
            if (::source.isInitialized) {
                source.close()
            }
            source = newSource
            bitmapCache.clear()
        }
    }

    suspend fun pageCount(): Int = source.pageCount()

    suspend fun loadPage(index: Int): Either<ChapterError, Bitmap> = withContext(decodeDispatcher) {
        val cacheKey = "page_$index"
        bitmapCache.get(cacheKey)?.let { return@withContext it.right() }

        source.openPage(index).flatMap { stream ->
            Either.catch {
                BufferedInputStream(stream, 64 * 1024).use { bis ->
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }

                    bis.mark(1024 * 1024)
                    BitmapFactory.decodeStream(bis, null, options)
                    bis.reset()

                    // Decode real com otimizações
                    options.apply {
                        inJustDecodeBounds = false
                        inPreferredConfig = Bitmap.Config.RGB_565
                        inDither = true
                        inMutable = true
                        inTempStorage = tempStorage
                        // Sample size simples por enquanto (pode ser melhorado se soubermos o screen size)
                        inSampleSize = calculateInSampleSize(options, 2000, 2000)
                    }

                    val bitmap = BitmapFactory.decodeStream(bis, null, options)
                        ?: throw IllegalStateException("Failed to decode bitmap")

                    bitmapCache.put(cacheKey, bitmap)
                    bitmap
                }
            }.mapLeft { ChapterError.ExtractionFailed(cause = it) }
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun prefetchWindow(
        center: Int,
        total: Int
    ) {
        // Prefetch 2 à frente e 2 atrás
        val range = ((center - 2)..(center + 2)).filter { it >= 0 && it < total }

        range.forEach { index ->
            CoroutineScope(context = Dispatchers.IO).launch {
                prefetchSemaphore.withPermit {
                    if (bitmapCache.get("page_$index") == null) {
                        loadPage(index)
                    }
                }
            }
        }
    }
}