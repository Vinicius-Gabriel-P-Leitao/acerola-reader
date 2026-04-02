package br.acerola.manga.logging

enum class LogLevel(val label: String) {
    VERBOSE("V"),
    DEBUG  ("D"),
    INFO   ("I"),
    WARN   ("W"),
    ERROR  ("E"),
    AUDIT  ("AUDIT")
}
