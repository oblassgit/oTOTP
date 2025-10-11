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

    fun saveSecret(service: String, secret: String) {
        sharedPreferences.edit { putString(service, secret) }
    }

    fun getSecret(service: String): String? {
        return sharedPreferences.getString(service, null)
    }

    fun getSecrets(): List<Secret> {
        return sharedPreferences.all.map{
            Secret(it.key, it.value as String)
        }
    }

    fun deleteSecret(service: String) {
        sharedPreferences.edit { remove(service) }
    }
}

data class Secret(
    val service: String,
    val secret: String,
)