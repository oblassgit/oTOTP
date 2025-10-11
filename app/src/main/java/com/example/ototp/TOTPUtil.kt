package com.example.ototp

import android.util.Base64
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.io.BaseEncoding
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TOTPUtil {
    val supportedDigitsList = listOf(4, 5, 6, 7, 8)

    fun generateTOTP(secret: String, digits: Int = 6, period: Int = 30): String {
        val key = Base64.decode(secret, Base64.DEFAULT)
        val time = System.currentTimeMillis() / 1000 / period
        val data = ByteBuffer.allocate(8).putLong(time).array()
        val hmac = Mac.getInstance("HmacSHA1")
        hmac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = hmac.doFinal(data)

        val offset = hash[hash.size - 1].toInt() and 0x0F
        val truncatedHash = hash.slice(offset until offset + 4).toByteArray()
        val code = ByteBuffer.wrap(truncatedHash).int and 0x7FFFFFFF
        return (code % Math.pow(10.0, digits.toDouble()).toInt()).toString().padStart(digits, '0')
    }

    fun generateTOTPBase32(
        secret: String,
        digits: Int = 6,
        period: Int = 30,
        timeSeconds: Long? = null // for testing
    ): String {
        // Base32 decoding (RFC 6238, Google Authenticator, etc.)
        val key = BaseEncoding.base32().decode(secret.uppercase().replace(" ", ""))
        val time = (timeSeconds ?: (System.currentTimeMillis() / 1000)) / period
        val data = ByteBuffer.allocate(8).putLong(time).array()
        val hmac = Mac.getInstance("HmacSHA1")
        hmac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = hmac.doFinal(data)

        val offset = hash[hash.size - 1].toInt() and 0x0F
        val truncatedHash = hash.slice(offset until offset + 4).toByteArray()
        val code = ByteBuffer.wrap(truncatedHash).int and 0x7FFFFFFF
        return (code % Math.pow(10.0, digits.toDouble()).toInt()).toString().padStart(digits, '0')
    }
}