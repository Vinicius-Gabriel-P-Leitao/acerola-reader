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
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfToCbzConverter
    @Inject
    constructor(
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
                    AcerolaLogger.d(TAG, "Opening PDF: ${pdfFile.name}", LogSource.REPOSITORY)
                    fileDescriptor = context.contentResolver.openFileDescriptor(pdfFile.uri, "r")
                        ?: return@withContext Either.Left(IoError.FileReadError(pdfFile.name ?: "unknown.pdf"))

                    pdfRenderer = PdfRenderer(fileDescriptor)

                    val pageCount = pdfRenderer.pageCount

                    AcerolaLogger.d(TAG, "PDF has $pageCount pages. Starting rendering...", LogSource.REPOSITORY)
                    
                    val cbzFile = folder.createFile("application/octet-stream", cbzFileName)
                        ?: return@withContext Either.Left(IoError.FileWriteError(path = cbzFileName))

                    val outputStream = context.contentResolver.openOutputStream(cbzFile.uri)
                        ?: return@withContext Either.Left(IoError.FileWriteError(path = cbzFileName))

                    ZipOutputStream(outputStream).use { zip ->
                        for (it in 0 until pageCount) {
                            val page = pdfRenderer.openPage(it)

                            // Increase scale for better reading quality
                            val width = (page.width * 2.0).toInt()
                            val height = (page.height * 2.0).toInt()

                            val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)

                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.WHITE)

                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()

                            val pageName = String.format(Locale.ENGLISH, "%04d.jpg", it + 1)
                            zip.putNextEntry(ZipEntry(pageName))
                            
                            // Comprime direto para o disco
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, zip)
                            
                            zip.closeEntry()
                            bitmap.recycle()

                            if ((it + 1) % 10 == 0 || it + 1 == pageCount) {
                                AcerolaLogger.v(TAG, "Rendered ${it + 1}/$pageCount pages", LogSource.REPOSITORY)
                            }
                        }
                    }

                    AcerolaLogger.d(TAG, "Rendering complete. CBZ file created successfully.", LogSource.REPOSITORY)
                    Either.Right(Unit)
                } catch (exception: Exception) {
                    AcerolaLogger.e(TAG, "PDF conversion failed: ${exception.message}", LogSource.REPOSITORY, throwable = exception)
                    Either.Left(IoError.FileWriteError(cbzFileName, exception))
                } finally {
                    pdfRenderer?.close()
                    fileDescriptor?.close()
                }
            }

        companion object {
            private const val TAG = "PdfToCbzConverter"
        }
    }
