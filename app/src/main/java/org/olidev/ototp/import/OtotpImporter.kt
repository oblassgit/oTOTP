package org.olidev.ototp.`import`

import org.json.JSONObject
import org.olidev.ototp.Algorithm
import org.olidev.ototp.TOTPToken
import java.io.InputStream

object OtotpImporter {

    /**
     * Parses an oTOTP backup file and returns a list of TOTP tokens.
     */
    fun parse(inputStream: InputStream): List<TOTPToken> {
        val json = JSONObject(inputStream.bufferedReader().readText())

        val version = json.optInt("version", 1)
        if (version != 1) {
            error("Unsupported oTOTP backup version: $version")
        }

        val tokensArray = json.optJSONArray("tokens") ?: return emptyList()
        val tokens = mutableListOf<TOTPToken>()

        for (i in 0 until tokensArray.length()) {
            val obj = tokensArray.getJSONObject(i)
            val secret = obj.optString("secret").takeIf { it.isNotBlank() } ?: continue
            val issuer = obj.optString("issuer").ifBlank { "Unknown" }
            val account = obj.optString("account").takeIf { it.isNotBlank() && it != "null" }
            val algorithm = Algorithm.fromString(obj.optString("algorithm"))
            val digits = obj.optInt("digits", 6).takeIf { it > 0 } ?: 6
            val period = obj.optInt("period", 30).takeIf { it > 0 } ?: 30

            tokens.add(
                TOTPToken(
                    id = null,
                    account = account,
                    secret = secret,
                    issuer = issuer,
                    algorithm = algorithm,
                    digits = digits,
                    period = period
                )
            )
        }

        return tokens
    }
}
