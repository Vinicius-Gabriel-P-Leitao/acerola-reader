package br.acerola.manga.pattern

enum class MediaFilePattern(val baseName: String) {
    COVER(baseName = "cover"),
    BANNER(baseName = "banner");

    val defaultFileName: String get() = "$baseName.png"

    fun matches(fileName: String): Boolean {
        val lower = fileName.lowercase()
        val base = lower.substringBeforeLast(".")
        val ext = lower.substringAfterLast(".", missingDelimiterValue = "")

        return base == baseName && ext in SUPPORTED_EXTENSIONS
    }

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("png", "jpg", "jpeg")

        fun from(fileName: String): MediaFilePattern? =
            entries.find { it.matches(fileName) }
    }
}
