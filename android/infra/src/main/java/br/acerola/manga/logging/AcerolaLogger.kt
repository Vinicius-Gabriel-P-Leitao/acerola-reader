package br.acerola.manga.logging

import android.util.Log
import br.acerola.manga.infra.BuildConfig

object AcerolaLogger {

    private const val PREFIX = "ACEROLA"
    private const val AUDIT_TAG = "AUDIT"

    fun v(
        tag: String,
        msg: String,
        source: LogSource = LogSource.UI
    ) =
        log(level = LogLevel.VERBOSE, tag = tag, msg = msg, source = source)

    fun d(
        tag: String,
        msg: String,
        source: LogSource = LogSource.UI
    ) =
        log(level = LogLevel.DEBUG, tag = tag, msg = msg, source = source)

    fun i(
        tag: String,
        msg: String,
        source: LogSource = LogSource.UI
    ) =
        log(level = LogLevel.INFO, tag = tag, msg = msg, source = source)

    fun w(
        tag: String,
        msg: String,
        source: LogSource = LogSource.UI,
        throwable: Throwable? = null
    ) =
        log(level = LogLevel.WARN, tag = tag, msg = msg, source = source, throwable = throwable)

    fun e(
        tag: String,
        msg: String,
        source: LogSource = LogSource.UI,
        throwable: Throwable? = null
    ) =
        log(level = LogLevel.ERROR, tag = tag, msg = msg, source = source, throwable = throwable)

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
            LogLevel.DEBUG -> Log.d(fullTag, formatted)
            LogLevel.INFO -> Log.i(fullTag, formatted)
            LogLevel.WARN -> Log.w(fullTag, formatted, throwable)
            LogLevel.ERROR -> Log.e(fullTag, formatted, throwable)
            LogLevel.AUDIT -> { /* usa audit() diretamente */
            }
        }
    }
}
