package br.acerola.comic.util.file

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import java.security.MessageDigest

fun DocumentFile.sha256(context: Context): String {
    val digest = MessageDigest.getInstance("SHA-256")

    context.contentResolver.openInputStream(uri)?.use { input ->
        val buffer = ByteArray(size = 8_192)
        var read = input.read(buffer)

        // `InputStream.read()` só sinaliza fim de arquivo com -1 — 0 é uma leitura
        // legítima de "nenhum byte disponível agora, mas não acabou", possível num
        // stream que atravessa outro processo (SAF/ContentResolver). Parar em `read <= 0`
        // (como estava antes) tratava esse 0 como EOF e cortava o hash de um arquivo
        // recém-escrito antes do fim, mesmo com o arquivo em disco correto e completo.
        while (read != -1) {
            if (read > 0) digest.update(buffer, 0, read)
            read = input.read(buffer)
        }
    } ?: error("The file could not be opened.")

    return digest.digest().joinToString(separator = "") { "%02x".format(it) }
}
