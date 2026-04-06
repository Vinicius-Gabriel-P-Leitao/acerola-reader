package br.acerola.comic.util

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import java.security.MessageDigest

fun DocumentFile.sha256(context: Context): String {
    val digest = MessageDigest.getInstance("SHA-256")

    context.contentResolver.openInputStream(uri)?.use { input ->
        val buffer = ByteArray(size = 8_192)
        var read = input.read(buffer)

        while (read > 0) {
            digest.update(buffer, 0, read)
            read = input.read(buffer)
        }
    } ?: error("Não foi possível abrir o arquivo")

    return digest.digest().joinToString(separator = "") { "%02x".format(it) }
}