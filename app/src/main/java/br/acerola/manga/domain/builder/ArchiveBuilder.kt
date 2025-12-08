package br.acerola.manga.domain.builder

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.domain.model.archive.MangaFolder
import br.acerola.manga.shared.config.preference.FileExtension
import br.acerola.manga.shared.util.detectTemplate

// TODO: Tratar erros melhor
object ArchiveBuilder {
    fun buildLibrary(context: Context, rootUri: Uri): List<MangaFolder> {
        val pickedDir = DocumentFile.fromTreeUri(context, rootUri) ?: return emptyList()

        return pickedDir.listFiles().filter { it.isDirectory }.map { folder ->
            val banner = folder.listFiles().firstOrNull { isBanner(file = it) }
            val cover = folder.listFiles().firstOrNull { isCover(file = it) }

            val firstChapter = folder.listFiles().firstOrNull { file ->
                file.isFile && FileExtension.isSupported(ext = file.name)
            }
            val detectedTemplate = firstChapter?.name?.let { detectTemplate(fileName = it) }

            MangaFolder(
                name = folder.name ?: "Unknown",
                path = folder.uri.toString(),
                cover = cover?.uri?.toString(),
                banner = banner?.uri?.toString(),
                chapterTemplate = detectedTemplate,
                lastModified = folder.lastModified(),
            )
        }
    }

    // TODO: Fazer "cover" ".jpg" ".png" ser um dicionario desse object, uma constante a ser usada.
    private fun isCover(file: DocumentFile): Boolean {
        val name = file.name?.lowercase() ?: return false
        return name.contains(other = "cover") && (name.endsWith(suffix = ".jpg") || name.endsWith(suffix = ".png"))
    }

    private fun isBanner(file: DocumentFile): Boolean {
        val name = file.name?.lowercase() ?: return false
        return name.contains(other = "banner") && (name.endsWith(suffix = ".jpg") || name.endsWith(suffix = ".png"))
    }
}