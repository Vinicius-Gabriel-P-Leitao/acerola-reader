package br.acerola.comic.util.p2p

import android.util.Base64
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import org.json.JSONObject

private const val CODE_PREFIX = "acerola1:"

/**
 * Encodes/decodes the pairing code exchanged via QR (or copy/paste). Mirrors exactly the
 * format used by the desktop app (`connection-code.utils.ts`), so a code generated on one
 * device can be pasted or scanned on the other:
 *
 * `acerola1:` + base64(JSON `{"i": id, "d": deviceId | null, "a": base64(addrs)}`)
 *
 * Rule: `addrs`'s raw bytes are never dropped straight into a `JSONArray` (that would become
 * a list of decimal numbers, ~4x larger and unreadable) — they're base64-encoded first, and
 * only then does the whole envelope (now all text) become JSON and get base64-encoded again.
 */
object PairingCode {
    data class Decoded(
        val id: String,
        val deviceId: String?,
        val addrs: ByteArray,
    )

    fun encode(
        id: String,
        deviceId: String?,
        addrs: ByteArray,
    ): String {
        val envelope =
            JSONObject().apply {
                put("i", id)
                put("d", deviceId ?: JSONObject.NULL)
                put("a", Base64.encodeToString(addrs, Base64.NO_WRAP))
            }

        val encoded = Base64.encodeToString(envelope.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return "$CODE_PREFIX$encoded"
    }

    fun decode(code: String): Decoded? {
        val trimmed = code.trim()
        if (!trimmed.startsWith(CODE_PREFIX)) return null

        return runCatching {
            val json = Base64.decode(trimmed.removePrefix(CODE_PREFIX), Base64.NO_WRAP).toString(Charsets.UTF_8)
            val envelope = JSONObject(json)

            val id = envelope.getString("i")
            val deviceId = if (envelope.isNull("d")) null else envelope.getString("d")
            val addrs = Base64.decode(envelope.getString("a"), Base64.NO_WRAP)

            Decoded(id = id, deviceId = deviceId, addrs = addrs)
        }.getOrElse { error ->
            // Expected for user-pasted garbage — not logged as an error, but still traced so
            // "why didn't my paste work" reports aren't a total black box.
            AcerolaLogger.d("PairingCode", "Failed to decode pairing code: $error", LogSource.UI)
            null
        }
    }

    /** Truncates a long id (peer id) for display: `909b713a…6511a8`. */
    fun shortId(
        id: String,
        keepStart: Int = 8,
        keepEnd: Int = 6,
    ): String {
        if (id.length <= keepStart + keepEnd + 1) return id
        return "${id.take(keepStart)}…${id.takeLast(keepEnd)}"
    }
}
