package com.example.ototp

import org.apache.commons.codec.binary.Base32
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