package br.acerola.comic.pattern

enum class MediaFilePattern(val baseName: String) {
    COVER(baseName = "cover"),
    BANNER(baseName = "banner");

    val defaultFileName: String get() = "$baseName.jpg"

    fun matches(fileName: String): Boolean {
        if (fileName.isBlank()) return false
        val nameWithoutExtension = fileName.substringBeforeLast(".")
        return nameWithoutExtension.equals(baseName, ignoreCase = true) && isImage(fileName)
    }

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")

        fun isImage(fileName: String?): Boolean {
            if (fileName.isNullOrBlank()) return false
            val ext = fileName.substringAfterLast(".", missingDelimiterValue = "").lowercase()
            return ext in SUPPORTED_EXTENSIONS
        }

        fun isCover(fileName: String?): Boolean {
            if (fileName.isNullOrBlank()) return false
            val lower = fileName.lowercase()
            return (lower.contains("cover") || lower.startsWith("folder") || lower.startsWith("front") || lower.startsWith("00")) && isImage(lower)
        }

        fun isBanner(fileName: String?): Boolean {
            if (fileName.isNullOrBlank()) return false
            val lower = fileName.lowercase()
            return lower.contains("banner") && isImage(lower)
        }

        fun from(fileName: String): MediaFilePattern? =
            entries.find { it.matches(fileName) }
    }
}
