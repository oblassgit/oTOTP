package com.example.ototp.`import`

import com.example.ototp.Algorithm
import com.example.ototp.TOTPToken
import com.example.ototp.TOTPUtil
import org.json.JSONObject
import java.io.InputStream

object TwoFasImporter {

    /**
     * Parses a 2FAS backup file and returns a list of TOTP tokens.
     * Encrypted backups are not supported and will result in an error.
     * HOTP tokens are skipped.
     */
    fun parse(inputStream: InputStream): List<TOTPToken> {
        val json = JSONObject(inputStream.bufferedReader().readText())

        if (json.has("reference")) {
            error("Encrypted 2FAS backups are not supported. Please export without a password.")
        }

        val servicesArray = json.optJSONArray("services") ?: return emptyList()
        val tokens = mutableListOf<TOTPToken>()

        for (i in 0 until servicesArray.length()) {
            val service = servicesArray.getJSONObject(i)
            val otp = service.optJSONObject("otp") ?: continue

            val tokenType = otp.optString("tokenType", "TOTP")
            if (!tokenType.equals("TOTP", ignoreCase = true)) continue

            val token = parseService(service, otp) ?: continue
            tokens.add(token)
        }

        return tokens
    }

    private fun parseService(service: JSONObject, otp: JSONObject): TOTPToken? {
        val link = otp.optString("link").takeIf { it.isNotBlank() }
        if (link != null) {
            return TOTPUtil.parse(link)
        }

        val secret = service.optString("secret").takeIf { it.isNotBlank() } ?: return null
        val issuer = otp.optString("issuer").ifBlank { service.optString("name").ifBlank { "Unknown" } }
        val account = otp.optString("account").ifBlank { otp.optString("label").takeIf { it.isNotBlank() } }
        val algorithm = Algorithm.fromString(otp.optString("algorithm"))
        val digits = otp.optInt("digits", 6).takeIf { it > 0 } ?: 6
        val period = otp.optInt("period", 30).takeIf { it > 0 } ?: 30

        return TOTPToken(
            id = null,
            account = account,
            secret = secret,
            issuer = issuer,
            algorithm = algorithm,
            digits = digits,
            period = period
        )
    }
}
