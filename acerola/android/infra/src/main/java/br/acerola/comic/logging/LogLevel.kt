package br.acerola.comic.logging

enum class LogLevel(
    val label: String,
) {
    VERBOSE("V"),
    DEBUG("D"),
    INFO("I"),
    WARN("W"),
    ERROR("E"),
    AUDIT("AUDIT"),
}
