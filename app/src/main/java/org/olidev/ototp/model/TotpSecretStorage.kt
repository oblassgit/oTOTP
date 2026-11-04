package org.olidev.ototp.model

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.nio.charset.StandardCharsets.UTF_8
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class TotpSecretStorage(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "totp_secret_ciphertext_v1",
        Context.MODE_PRIVATE
    )

    fun saveSecret(id: Long, secret: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(aad(id))

        val encrypted = cipher.doFinal(secret.toByteArray(UTF_8))
        val iv = cipher.iv
        require(iv.size <= 255) { "Unexpected AES-GCM IV length" }

        val value = byteArrayOf(iv.size.toByte()) + iv + encrypted
        prefs.edit { putString(keyFor(id), Base64.encodeToString(value, Base64.NO_WRAP)) }
    }

    fun getSecret(id: Long): String? {
        val encoded = prefs.getString(keyFor(id), null) ?: return null
        val value = Base64.decode(encoded, Base64.NO_WRAP)

        val ivLength = value[0].toInt() and 0xFF
        require(ivLength > 0 && value.size >= 1 + ivLength + GCM_TAG_BYTES) {
            "Invalid encrypted secret"
        }

        val iv = value.copyOfRange(1, 1 + ivLength)
        val encrypted = value.copyOfRange(1 + ivLength, value.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        cipher.updateAAD(aad(id))

        return String(cipher.doFinal(encrypted), UTF_8)
    }

    fun deleteSecret(id: Long) {
        prefs.edit { remove(keyFor(id)) }
    }

    private fun keyFor(id: Long) = "totp_secret_$id"

    private fun aad(id: Long) = keyFor(id).toByteArray(UTF_8)

    private fun getOrCreateKey(): SecretKey = synchronized(keyLock) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return@synchronized it }

        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val KEY_ALIAS = "totp_secret_key_v1"
        const val GCM_TAG_BYTES = 16
        val keyLock = Any()
    }
}