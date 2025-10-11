package com.example.ototp

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit

class TotpSecretStorage(context: Context) {
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secret_shared_prefs",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSecret(id: Long, secret: String) {
        sharedPreferences.edit { putString("totp_secret_$id", secret) }
    }

    fun getSecret(id: Long): String? {
        return sharedPreferences.getString("totp_secret_$id", null)
    }

    fun getSecrets(): List<Secret> {
        return sharedPreferences.all.map{
            Secret(it.key, it.value as String)
        }
    }

    fun deleteSecret(id: Long) {
        sharedPreferences.edit { remove("totp_secret_$id") }
    }
}

data class Secret(
    val service: String,
    val secret: String,
)