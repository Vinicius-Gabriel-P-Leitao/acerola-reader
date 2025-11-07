package br.acerola.manga.shared.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter

// TODO: Adicionar validaçõa de erro personalizada.
fun uriToPainter(context: Context, uri: Uri): BitmapPainter? {
    try {
        context.contentResolver.query(
            uri, arrayOf(DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val mimeType = cursor.getString(0)

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    return null
                }

                if (mimeType?.startsWith("image/") == false) {
                    return null
                }
            }
        }

        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bitmap = BitmapFactory.decodeStream(stream) ?: return null
            return BitmapPainter(image = bitmap.asImageBitmap())
        }
    } catch (e: Exception) {
        Log.e("uriToPainter", "Error processing URI: $uri", e)
    }
    return null
}