package com.example.ototp.`import`

import android.net.Uri
import com.example.ototp.Algorithm
import com.example.ototp.TOTPToken
import org.apache.commons.codec.binary.Base32
import java.net.URLDecoder

/**
 * Parses a Google Authenticator export QR code payload.
 *
 * Google Authenticator exports use the scheme:
 *   otpauth-migration://offline?data=<URL-encoded Base64 protobuf>
 *
 * The protobuf schema (MigrationPayload) contains a repeated OtpParameters message:
 *   Field 1 (bytes):  secret
 *   Field 2 (string): name (account)
 *   Field 3 (string): issuer
 *   Field 4 (varint): algorithm (1=SHA1, 2=SHA256, 3=SHA512)
 *   Field 5 (varint): digits (1=6, 2=8)
 *   Field 6 (varint): otp_type (1=HOTP, 2=TOTP)
 *   Field 7 (int64):  counter (HOTP only)
 */
object GoogleAuthImporter {

    fun parse(url: String): List<TOTPToken> {
        val uri = Uri.parse(url)
        require(uri.scheme == "otpauth-migration") {
            "Not a Google Authenticator export QR code."
        }

        val dataParam = uri.getQueryParameter("data")
            ?: error("Missing 'data' parameter in Google Authenticator export URL.")

        // The data parameter may be URL-encoded
        val base64Data = URLDecoder.decode(dataParam, "UTF-8")
        val protoBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)

        return decodeMigrationPayload(protoBytes)
    }

    private fun decodeMigrationPayload(bytes: ByteArray): List<TOTPToken> {
        val tokens = mutableListOf<TOTPToken>()
        val reader = ProtoReader(bytes)

        while (reader.hasMore()) {
            val (fieldNumber, wireType) = reader.readTag()
            if (fieldNumber == 1 && wireType == 2) {
                // repeated OtpParameters otp_parameters = 1
                val messageBytes = reader.readBytes()
                decodeOtpParameters(messageBytes)?.let { tokens.add(it) }
            } else {
                reader.skipField(wireType)
            }
        }

        return tokens
    }

    private fun decodeOtpParameters(bytes: ByteArray): TOTPToken? {
        val reader = ProtoReader(bytes)

        var secret: ByteArray? = null
        var name: String? = null
        var issuer: String? = null
        var algorithm = 1  // default SHA1
        var digits = 1     // default 6
        var otpType = 2    // default TOTP

        while (reader.hasMore()) {
            val (fieldNumber, wireType) = reader.readTag()
            when (fieldNumber) {
                1 -> secret = reader.readBytes()
                2 -> name = reader.readString()
                3 -> issuer = reader.readString()
                4 -> algorithm = reader.readVarint().toInt()
                5 -> digits = reader.readVarint().toInt()
                6 -> otpType = reader.readVarint().toInt()
                7 -> reader.readVarint() // counter, skip
                else -> reader.skipField(wireType)
            }
        }

        // Skip HOTP tokens
        if (otpType != 2) return null

        val secretBytes = secret ?: return null
        val secretBase32 = Base32().encodeToString(secretBytes).trimEnd('=')

        // Parse label into issuer/account
        val rawIssuer: String
        val account: String?
        if (!name.isNullOrBlank()) {
            val colon = name.indexOf(':')
            if (colon != -1) {
                rawIssuer = name.substring(0, colon).trim()
                account = name.substring(colon + 1).trim().ifBlank { null }
            } else {
                rawIssuer = name
                account = null
            }
        } else {
            rawIssuer = "Unknown"
            account = null
        }

        val finalIssuer = when {
            !issuer.isNullOrBlank() -> issuer
            rawIssuer.isNotBlank() -> rawIssuer
            else -> "Unknown"
        }

        return TOTPToken(
            id = null,
            account = account,
            secret = secretBase32,
            issuer = finalIssuer,
            algorithm = when (algorithm) {
                2 -> Algorithm.SHA256
                3 -> Algorithm.SHA512
                else -> Algorithm.SHA1
            },
            digits = when (digits) {
                2 -> 8
                else -> 6
            },
            period = 30
        )
    }

    // Minimal protobuf reader
    private class ProtoReader(private val bytes: ByteArray) {
        private var pos = 0

        fun hasMore() = pos < bytes.size

        fun readTag(): Pair<Int, Int> {
            val v = readVarint().toInt()
            return (v ushr 3) to (v and 0x7)
        }

        fun readVarint(): Long {
            var result = 0L
            var shift = 0
            while (true) {
                val b = bytes[pos++].toInt() and 0xFF
                result = result or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) break
                shift += 7
            }
            return result
        }

        fun readBytes(): ByteArray {
            val length = readVarint().toInt()
            val data = bytes.copyOfRange(pos, pos + length)
            pos += length
            return data
        }

        fun readString(): String = String(readBytes(), Charsets.UTF_8)

        fun skipField(wireType: Int) {
            when (wireType) {
                0 -> readVarint()         // varint
                1 -> pos += 8             // 64-bit
                2 -> readBytes()          // length-delimited
                5 -> pos += 4             // 32-bit
                else -> error("Unsupported wire type: $wireType")
            }
        }
    }
}
