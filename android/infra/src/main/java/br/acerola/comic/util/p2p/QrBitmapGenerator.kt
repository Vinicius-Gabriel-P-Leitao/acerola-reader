package br.acerola.comic.util.p2p

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

private const val COLOR_BLACK = 0xFF000000.toInt()
private const val COLOR_WHITE = 0xFFFFFFFF.toInt()

object QrBitmapGenerator {
    fun generate(
        content: String,
        sizePx: Int = 512,
    ): Bitmap {
        val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (column in 0 until sizePx) {
            for (row in 0 until sizePx) {
                bitmap.setPixel(column, row, if (matrix[column, row]) COLOR_BLACK else COLOR_WHITE)
            }
        }
        return bitmap
    }
}
