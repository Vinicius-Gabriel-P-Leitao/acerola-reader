package br.acerola.manga.service.cache

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import br.acerola.manga.error.message.ChapterError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PageCacheHandler @Inject constructor() {
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

        override fun put(key: Int, value: ByteArray): ByteArray? {
            currentSize += value.size
            return super.put(key, value)
        }

        override fun clear() {
            currentSize = 0
            super.clear()
        }
    }

    fun get(index: Int): Either<ChapterError, ByteArray> {
        return cache[index]?.right()
            ?: ChapterError.UnexpectedError(cause = Throwable(message = "Page $index not found in cache")).left()
    }

    fun put(index: Int, data: ByteArray) {
        cache[index] = data
    }

    fun clear() {
        cache.clear()
    }
}
