package org.olidev.ototp.export

import org.json.JSONArray
import org.json.JSONObject
import org.olidev.ototp.TOTPToken

object OtotpExporter {

    /**
     * Serializes a list of TOTP tokens to the oTOTP backup JSON format.
     * The resulting string should be written to a file with the .ototp extension.
     * Note: secrets are stored in plain text — advise users to keep the file secure.
     */
    fun serialize(tokens: List<TOTPToken>): String {
        val tokensArray = JSONArray()
        for (token in tokens) {
            val obj = JSONObject().apply {
                put("issuer", token.issuer)
                put("account", token.account ?: JSONObject.NULL)
                put("secret", token.secret)
                put("algorithm", token.algorithm.name)
                put("digits", token.digits)
                put("period", token.period)
            }
            tokensArray.put(obj)
        }

        return JSONObject().apply {
            put("version", 1)
            put("app", "oTOTP")
            put("tokens", tokensArray)
        }.toString(2)
    }
}
