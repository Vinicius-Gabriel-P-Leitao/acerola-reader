package br.acerola.comic.service.network

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import br.acerola.comic.config.preference.ComicDirectoryPreference
import br.acerola.comic.local.dao.archive.ChapterArchiveDao
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.usecase.network.RegisterSyncedChapterUseCase
import br.acerola.comic.util.file.sha256
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import p2p.FfiFileManifestEntry
import p2p.FileSyncProvider
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

private const val SYNCED_FOLDER_NAME = "synced"

/**
 * Kotlin implementation of [FileSyncProvider] used by the Rust protocol `acerola/sync-files/1`.
 * Reads/writes chapter files via SAF/DocumentFile in chunks — Rust never sees a stream
 * directly, only opaque `Long` handles, so the handle maps here are the "memory" of
 * transfers in flight.
 */
@Singleton
class FileSyncProviderImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val comicDirectoryDao: ComicDirectoryDao,
        private val chapterArchiveDao: ChapterArchiveDao,
        private val registerSyncedChapter: RegisterSyncedChapterUseCase,
    ) : FileSyncProvider {
        private data class ReadHandle(
            val stream: InputStream,
        )

        private data class WriteHandle(
            val comicName: String,
            val chapter: String,
            val fileName: String,
            val expectedChecksum: String,
            val tempFile: DocumentFile,
            val comicFolder: DocumentFile,
            val outputStream: OutputStream,
        )

        private val readHandles = ConcurrentHashMap<Long, ReadHandle>()
        private val writeHandles = ConcurrentHashMap<Long, WriteHandle>()
        private val handleCounter = AtomicLong(1)

        @Volatile
        private var cachedLibraryRoot: DocumentFile? = null

        private suspend fun libraryRoot(): DocumentFile? {
            cachedLibraryRoot?.let { return it }
            val uriString = ComicDirectoryPreference.folderUriFlow(context).first() ?: return null
            val root = DocumentFile.fromTreeUri(context, Uri.parse(uriString)) ?: return null
            cachedLibraryRoot = root
            return root
        }

        override fun getFileManifest(): List<FfiFileManifestEntry> =
            runBlocking {
                comicDirectoryDao.getAllDirectories().first().flatMap { comic ->
                    chapterArchiveDao.getChaptersListByDirectoryId(comic.id).mapNotNull { chapter ->
                        val file = DocumentFile.fromSingleUri(context, Uri.parse(chapter.path))
                        if (file == null || !file.exists()) return@mapNotNull null

                        val checksum =
                            chapter.checksum ?: run {
                                val computed = file.sha256(context)
                                chapterArchiveDao.update(chapter.copy(checksum = computed))
                                computed
                            }

                        FfiFileManifestEntry(
                            comicName = comic.name,
                            chapter = chapter.chapter,
                            fileName = file.name ?: chapter.chapter,
                            checksum = checksum,
                            sizeBytes = file.length().toULong(),
                        )
                    }
                }
            }

        override fun openChapterForRead(
            comicName: String,
            chapter: String,
        ): Long =
            runBlocking {
                val comic = comicDirectoryDao.getDirectoryByName(comicName) ?: return@runBlocking -1L
                val chapterArchive =
                    chapterArchiveDao.getChapterByDirectoryAndChapter(comic.id, chapter) ?: return@runBlocking -1L
                val stream =
                    context.contentResolver.openInputStream(Uri.parse(chapterArchive.path)) ?: return@runBlocking -1L

                val handle = handleCounter.getAndIncrement()
                readHandles[handle] = ReadHandle(stream)
                handle
            }

        override fun readChapterChunk(
            handle: Long,
            chunkSize: UInt,
        ): ByteArray {
            val readHandle = readHandles[handle] ?: return ByteArray(0)
            val buffer = ByteArray(chunkSize.toInt())
            val read = readHandle.stream.read(buffer)
            if (read <= 0) return ByteArray(0)
            return if (read == buffer.size) buffer else buffer.copyOf(read)
        }

        override fun closeReadHandle(handle: Long) {
            runCatching { readHandles.remove(handle)?.stream?.close() }
        }

        override fun beginChapterWrite(
            comicName: String,
            chapter: String,
            fileName: String,
            expectedChecksum: String,
            sizeBytes: ULong,
        ): Long =
            runBlocking {
                val root = libraryRoot() ?: return@runBlocking -1L
                val syncedFolder = root.findFile(SYNCED_FOLDER_NAME) ?: root.createDirectory(SYNCED_FOLDER_NAME)
                val comicFolder = syncedFolder?.findFile(comicName) ?: syncedFolder?.createDirectory(comicName)
                if (comicFolder == null) return@runBlocking -1L

                // Final name comes from the peer (preserves the original .cbz/.cbr extension);
                // if it comes empty (the "chapter unavailable" placeholder), fall back to the
                // chapter label.
                val finalName = fileName.ifBlank { chapter }
                val tempName = "$finalName.part"
                comicFolder.findFile(tempName)?.delete()
                val tempFile =
                    comicFolder.createFile("application/octet-stream", tempName) ?: return@runBlocking -1L
                val outputStream =
                    context.contentResolver.openOutputStream(tempFile.uri, "wt") ?: run {
                        tempFile.delete()
                        return@runBlocking -1L
                    }

                val handle = handleCounter.getAndIncrement()
                writeHandles[handle] =
                    WriteHandle(comicName, chapter, finalName, expectedChecksum, tempFile, comicFolder, outputStream)
                handle
            }

        override fun writeChapterChunk(
            handle: Long,
            bytes: ByteArray,
        ): Boolean {
            val writeHandle = writeHandles[handle] ?: return false
            return try {
                writeHandle.outputStream.write(bytes)
                true
            } catch (error: IOException) {
                AcerolaLogger.e("FileSyncProvider", "Failed to write chunk for handle $handle", LogSource.NETWORK, error)
                false
            }
        }

        override fun finalizeChapterWrite(handle: Long): Boolean =
            runBlocking {
                val writeHandle = writeHandles.remove(handle) ?: return@runBlocking false
                runCatching { writeHandle.outputStream.close() }

                val actualChecksum = writeHandle.tempFile.sha256(context)
                if (actualChecksum != writeHandle.expectedChecksum) {
                    writeHandle.tempFile.delete()
                    return@runBlocking false
                }

                writeHandle.comicFolder.findFile(writeHandle.fileName)?.delete()
                if (!writeHandle.tempFile.renameTo(writeHandle.fileName)) {
                    writeHandle.tempFile.delete()
                    return@runBlocking false
                }

                val finalFile = writeHandle.comicFolder.findFile(writeHandle.fileName) ?: writeHandle.tempFile
                registerSyncedChapter(
                    comicName = writeHandle.comicName,
                    chapter = writeHandle.chapter,
                    checksum = actualChecksum,
                    fileUri = finalFile.uri.toString(),
                    comicFolderUri = writeHandle.comicFolder.uri.toString(),
                )
                true
            }

        override fun abortChapterWrite(handle: Long) {
            val writeHandle = writeHandles.remove(handle) ?: return
            runCatching { writeHandle.outputStream.close() }
            writeHandle.tempFile.delete()
        }
    }
