package it.refertoelettronico.integration

import it.refertoelettronico.model.MatchConfig
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.Base64

/** Small, local pre-match payloads from a browser. Never downloads or saves a match. */
object MatchSetupLink {
    const val SCHEME = "refertovolley"
    const val HOST = "import-match"
    const val MAX_PAYLOAD_CHARS = 16_000

    fun decode(address: String): MatchConfig {
        require(address.length <= MAX_PAYLOAD_CHARS + 128) { "Collegamento troppo lungo. Importa il file JSON." }
        val uri = URI(address)
        require(uri.scheme == SCHEME && uri.host == HOST && uri.rawPath.isNullOrEmpty() &&
            uri.rawUserInfo == null && uri.port == -1 && uri.rawFragment == null) { "Collegamento gara non valido." }
        val query = uri.rawQuery.orEmpty()
        require(query.startsWith("data=")) { "Dati gara mancanti nel collegamento." }
        val encoded = query.removePrefix("data=")
        require(encoded.length in 1..MAX_PAYLOAD_CHARS && encoded.matches(Regex("[A-Za-z0-9_-]+"))) { "Dati del collegamento non validi." }
        val bytes = Base64.getUrlDecoder().decode(encoded)
        val json = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
        return MatchSetupImport.decode(json)
    }
}
