package br.acerola.manga.logging

import android.util.Log
import br.acerola.manga.infrastructure.BuildConfig

object AcerolaLogger {

    private const val PREFIX = "ACEROLA"
    private const val AUDIT_TAG = "AUDIT"

    fun v(tag: String, msg: String, source: LogSource = LogSource.UI) =
        log(LogLevel.VERBOSE, tag, msg, source)

    fun d(tag: String, msg: String, source: LogSource = LogSource.UI) =
        log(LogLevel.DEBUG, tag, msg, source)

    fun i(tag: String, msg: String, source: LogSource = LogSource.UI) =
        log(LogLevel.INFO, tag, msg, source)

    fun w(tag: String, msg: String, source: LogSource = LogSource.UI, t: Throwable? = null) =
        log(LogLevel.WARN, tag, msg, source, t)

    fun e(tag: String, msg: String, source: LogSource = LogSource.UI, t: Throwable? = null) =
        log(LogLevel.ERROR, tag, msg, source, t)

    /**
     * Audit: ações do usuário. Usa tag própria para filtrar separado no Logcat.
     * Ex: adb logcat -s AUDIT:*
     */
    fun audit(
        tag: String,
        msg: String,
        source: LogSource,
        extras: Map<String, String> = emptyMap()
    ) {
        if (!BuildConfig.DEBUG) return
        val extrasStr = if (extras.isEmpty()) "" else " | ${extras.entries.joinToString { "${it.key}=${it.value}" }}"
        Log.i("$AUDIT_TAG/$tag", "[${source}] $msg$extrasStr")
    }

    private fun log(
        tag: String,
        msg: String,
        level: LogLevel,
        source: LogSource,
        throwable: Throwable? = null
    ) {
        if (!BuildConfig.DEBUG) return

        val fullTag = "$PREFIX/$tag"
        val formatted = "[${source.name}] $msg"

        when (level) {
            LogLevel.VERBOSE -> Log.v(fullTag, formatted)
            LogLevel.DEBUG   -> Log.d(fullTag, formatted)
            LogLevel.INFO    -> Log.i(fullTag, formatted)
            LogLevel.WARN    -> Log.w(fullTag, formatted, throwable)
            LogLevel.ERROR   -> Log.e(fullTag, formatted, throwable)
            LogLevel.AUDIT   -> { /* usa audit() diretamente */ }
        }
    }
}
