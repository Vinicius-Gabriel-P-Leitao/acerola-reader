package br.acerola.manga.service.cache

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import br.acerola.manga.error.message.ChapterError
import javax.inject.Inject
import javax.inject.Singleton

// TODO: Otimizar o cache das paginas, isso é bem importante
@Singleton
class PageCacheService @Inject constructor() {
    private val maxSizeBytes = 60L * 1024 * 1024 // 60MB

    /**
     * Usa LinkedHashMap pela facilidade de manter um histório default da estrutura
     */
    private val cache = object : LinkedHashMap<Int, ByteArray>(0, 0.75f, true) {
        private var currentSize = 0L

        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<Int, ByteArray>
        ): Boolean {
            if (currentSize > maxSizeBytes) {
                currentSize -= eldest.value.size
                return true
            }

            return false
        }

        fun putPage(index: Int, data: ByteArray) {
            put(key = index, value = data)
            currentSize += data.size
        }
    }

    fun get(index: Int): Either<ChapterError, ByteArray> {
        return cache[index]?.right()
            ?: ChapterError.UnexpectedError(cause = Throwable(message = "Page $index not found in cache")).left()
    }

    fun put(index: Int, data: ByteArray) {
        cache.putPage(index, data)
    }

    fun clear() {
        cache.clear()
    }
}
