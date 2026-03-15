package br.acerola.manga.logging

data class LogEvent(
    val level: LogLevel,
    val tag: String,
    val message: String,
    val source: LogSource,
    val throwable: Throwable? = null,
    val extras: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)
