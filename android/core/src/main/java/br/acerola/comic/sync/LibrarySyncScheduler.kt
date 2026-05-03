package br.acerola.comic.sync

import android.net.Uri

interface LibrarySyncScheduler {
    fun enqueueIncremental(baseUri: Uri?)
    fun enqueueRefresh(baseUri: Uri?)
    fun enqueueRebuild(baseUri: Uri?)
    fun enqueueSpecific(comicId: Long, baseUri: Uri?)
}
