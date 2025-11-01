package br.acerola.manga.domain.service.archive

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

// TODO: Fazer uma busca mais bruta, necessário para pegar arquivos de pastas com nomes corretos e pegar arquivos
//  também tipo "Aijin" arquivos Cap 1.cbz ou 1.cbz, criar váriações para isso, ser o mais genêrico.
fun scanFolder(context: Context, folderUri: Uri): List<Uri> {
    val pickedDir = DocumentFile.fromTreeUri(context, folderUri) ?: return emptyList()
    return pickedDir.listFiles().filter {
        it.isFile && (it.name!!.endsWith(suffix = ".cbz") || it.name!!.endsWith(suffix = ".cbr"))
    }.map { it.uri }
}