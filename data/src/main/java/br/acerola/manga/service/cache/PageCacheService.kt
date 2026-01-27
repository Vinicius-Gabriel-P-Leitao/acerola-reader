package br.acerola.manga.service.cache

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PageCacheService @Inject constructor() {

    private val maxSizeBytes = 60L * 1024 * 1024 // 60MB

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
            put(index, data)
            currentSize += data.size
        }
    }

    fun get(index: Int): ByteArray? = cache[index]

    fun put(index: Int, data: ByteArray) {
        cache.putPage(index, data)
    }

    fun clear() {
        cache.clear()
    }
}
