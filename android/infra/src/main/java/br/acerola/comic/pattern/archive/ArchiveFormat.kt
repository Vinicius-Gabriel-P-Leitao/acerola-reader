package br.acerola.comic.pattern.archive

enum class ArchiveFormat(
    val extension: String,
    val indexable: Boolean = true,
) {
    CBZ(extension = ".cbz"),
    CBR(extension = ".cbr"),
    PDF(extension = ".pdf", indexable = false),
    ;

    companion object {
        fun isSupported(ext: String?): Boolean {
            if (ext.isNullOrBlank()) return false
            val cleanExtension = ext.substringAfterLast(delimiter = '.').lowercase()
            return entries.any { it.name.equals(other = cleanExtension, ignoreCase = true) }
        }

        fun isIndexable(ext: String?): Boolean {
            if (ext.isNullOrBlank()) return false
            val cleanExtension = ext.substringAfterLast(delimiter = '.').lowercase()
            return entries.find { it.name.equals(other = cleanExtension, ignoreCase = true) }?.indexable ?: false
        }
    }
}
