package br.acerola.manga.service.cache

import android.graphics.Bitmap
import androidx.collection.LruCache
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BitmapCacheHandler @Inject constructor() {

    private val maxMemory = Runtime.getRuntime().maxMemory()
    private val cacheSize = (maxMemory / 8).toInt()

    private val cache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    fun get(key: String): Bitmap? {
        return cache[key]
    }

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    fun remove(key: String) {
        cache.remove(key)
    }

    fun clear() {
        cache.evictAll()
    }
}
