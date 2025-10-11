package com.example.ototp

import java.net.URLDecoder
import android.net.Uri

object OTPAuthParser {
    fun parse(url: String): TOTPToken? {
        try {
            val uri = Uri.parse(url)
            if (uri.scheme != "otpauth") return null
            if (uri.host != "totp") return null

            val labelPart = uri.path?.removePrefix("/")?.let { URLDecoder.decode(it, "UTF-8") } ?: ""

            // Split label into issuer and account if possible
            val labelIssuer: String
            val accountName: String?
            val colonIndex = labelPart.indexOf(':')

            if (colonIndex != -1) {
                labelIssuer = labelPart.substring(0, colonIndex)
                accountName = labelPart.substring(colonIndex + 1)
            } else {
                labelIssuer = labelPart
                accountName = null
            }

            val secret = uri.getQueryParameter("secret") ?: return null
            val paramIssuer = uri.getQueryParameter("issuer")
            val algorithm = uri.getQueryParameter("algorithm")
            val digits = uri.getQueryParameter("digits")?.toIntOrNull()
            val period = uri.getQueryParameter("period")?.toIntOrNull()

            // Per spec, prefer query issuer, fallback to label issuer, else fallback to label/accountName, else "Unknown"
            val finalIssuer = paramIssuer ?: labelIssuer

            return TOTPToken(
                id = null,
                account = accountName,
                secret = secret,
                issuer = finalIssuer,
                algorithm = Algorithm.fromString(algorithm),
                digits = digits,
                period = period
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}

data class TOTPToken(
    val id: Long?,
    val account: String?,     // This is the account/user name
    val secret: String,
    val issuer: String,
    val algorithm: Algorithm,
    var digits: Int? = 6,
    var period: Int? = 30
)