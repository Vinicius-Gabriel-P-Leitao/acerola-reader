package br.acerola.manga.config.pattern

enum class ArchiveFormatPattern(val extension: String) {
    CBZ(extension = ".cbz"), CBR(extension = ".cbr");

    companion object {
        fun isSupported(ext: String?): Boolean {
            if (ext.isNullOrBlank()) return false
            val cleanExtension = ext.substringAfterLast(delimiter = '.').lowercase()
            return entries.any { it.name.equals(other = cleanExtension, ignoreCase = true) }
        }
    }
}