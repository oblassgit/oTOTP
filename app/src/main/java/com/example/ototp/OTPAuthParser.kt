package com.example.ototp

import java.net.URLDecoder
import android.net.Uri

object OTPAuthParser {
    fun parse(url: String): TOTPToken? {
        try {
            val uri = Uri.parse(url)
            if (uri.scheme != "otpauth") return null
            if (uri.host != "totp") return null

            // Label is the path without the leading slash, URL decoded
            val label = uri.path?.removePrefix("/")?.let { URLDecoder.decode(it, "UTF-8") } ?: ""

            val secret = uri.getQueryParameter("secret") ?: return null
            val issuer = uri.getQueryParameter("issuer")
            val algorithm = uri.getQueryParameter("algorithm")
            val digits = uri.getQueryParameter("digits")?.toIntOrNull()
            val period = uri.getQueryParameter("period")?.toLongOrNull()

            return TOTPToken(
                label = label,
                secret = secret,
                issuer = issuer,
                algorithm = algorithm,
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
    val label: String,
    val secret: String,
    val issuer: String?,
    val algorithm: String?,
    val digits: Int?,
    val period: Long?
)