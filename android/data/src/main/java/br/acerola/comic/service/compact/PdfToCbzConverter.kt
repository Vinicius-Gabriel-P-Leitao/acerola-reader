package br.acerola.comic.service.compact

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.comic.error.message.IoError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfToCbzConverter
    @Inject
    constructor(
        private val archiveCompactService: CbzCompressor,
        @param:ApplicationContext private val context: Context,
    ) {
        suspend fun convertPdfToCbz(
            folder: DocumentFile,
            pdfFile: DocumentFile,
            cbzFileName: String,
        ): Either<IoError, Unit> =
            withContext(Dispatchers.IO) {
                var fileDescriptor: ParcelFileDescriptor? = null
                var pdfRenderer: PdfRenderer? = null
                try {
                    fileDescriptor = context.contentResolver.openFileDescriptor(pdfFile.uri, "r")
                        ?: return@withContext Either.Left(IoError.FileReadError(pdfFile.name ?: "unknown.pdf"))

                    pdfRenderer = PdfRenderer(fileDescriptor)

                    val pageCount = pdfRenderer.pageCount
                    val pageEntries = mutableListOf<Pair<String, ByteArray>>()

                    for (i in 0 until pageCount) {
                        val page = pdfRenderer.openPage(i)

                        // Increase scale for better reading quality
                        val width = (page.width * 2.0).toInt()
                        val height = (page.height * 2.0).toInt()

                        val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)

                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)

                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()

                        val outputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        val byteArray = outputStream.toByteArray()
                        bitmap.recycle()

                        val pageName = String.format("%04d.jpg", i + 1)
                        pageEntries.add(Pair(pageName, byteArray))
                    }

                    archiveCompactService.createCbz(folder, cbzFileName, pageEntries)
                } catch (exception: Exception) {
                    Either.Left(IoError.FileWriteError(cbzFileName, exception))
                } finally {
                    pdfRenderer?.close()
                    fileDescriptor?.close()
                }
            }
    }
