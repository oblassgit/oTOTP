package com.example.ototp

import androidx.core.net.toUri
import org.apache.commons.codec.binary.Base32
import java.net.URLDecoder
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TOTPUtil {

    val supportedDigitsList = listOf(4, 5, 6, 7, 8)

    fun generateTOTPBase32(
        secret: String,
        digits: Int,
        period: Int,
        algorithm: Algorithm = Algorithm.SHA1,
        timeSeconds: Long? = null // for testing
    ): String {
        // Base32 decoding (RFC 6238, Google Authenticator, etc.)
        val key = Base32().decode(secret.uppercase().replace(" ", ""))
        val time = (timeSeconds ?: (System.currentTimeMillis() / 1000)) / period
        val data = ByteBuffer.allocate(8).putLong(time).array()
        val hmac = Mac.getInstance(algorithm.hmacName)
        hmac.init(SecretKeySpec(key, algorithm.hmacName))
        val hash = hmac.doFinal(data)

        val offset = hash[hash.size - 1].toInt() and 0x0F
        val truncatedHash = hash.slice(offset until offset + 4).toByteArray()
        val code = ByteBuffer.wrap(truncatedHash).int and 0x7FFFFFFF
        return (code % Math.pow(10.0, digits.toDouble()).toInt()).toString().padStart(digits, '0')
    }

    fun parse(url: String): TOTPToken? {
        try {
            val uri = url.toUri()
            if (uri.scheme != "otpauth") return null
            if (uri.host != "totp") return null

            val labelPart =
                uri.path?.removePrefix("/")?.let { URLDecoder.decode(it, "UTF-8") } ?: ""

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
            val digits = uri.getQueryParameter("digits")?.toIntOrNull() ?: 6 //default to 6 digits
            val period =
                uri.getQueryParameter("period")?.toIntOrNull() ?: 30 //default to 30 seconds

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

enum class Algorithm(val hmacName: String) {
    SHA1("HmacSHA1"),
    SHA256("HmacSHA256"),
    SHA512("HmacSHA512");

    companion object {
        fun fromString(value: String?): Algorithm =
            when (value?.uppercase()) {
                "SHA1", "HMACSHA1" -> SHA1
                "SHA256", "HMACSHA256" -> SHA256
                "SHA512", "HMACSHA512" -> SHA512
                else -> SHA1 // default/fallback
            }
    }
}

data class TOTPToken(
    val id: Long?,
    val account: String?,
    val secret: String,
    val issuer: String,
    val algorithm: Algorithm,
    var digits: Int = 6,
    var period: Int = 30
)