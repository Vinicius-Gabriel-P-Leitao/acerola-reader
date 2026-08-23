package br.acerola.comic.pattern.archive

object SpecialArchive {
    private val KEYWORDS = setOf("special", "extra", "oneshot", "especial")

    fun isSpecial(name: String): Boolean {
        if (name.isBlank()) return false
        val lower = name.lowercase()
        return KEYWORDS.any { lower.contains(it) }
    }
}
