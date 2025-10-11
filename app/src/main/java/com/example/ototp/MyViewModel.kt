package com.example.ototp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MyViewModel(
    val totpSecretStorage: TotpSecretStorage,
): ViewModel() {
    private val _secrets = MutableStateFlow<List<Secret>>(emptyList())
    val secrets: StateFlow<List<Secret>> = _secrets

    var tokenToEdit: TOTPToken? by mutableStateOf(null)

    fun saveSecret(service: String, secret: String) {
        totpSecretStorage.saveSecret(service, secret)
        loadSecrets()
    }

    fun getSecret(service: String): String? {
        return totpSecretStorage.getSecret(service)
    }

    fun deleteSecret(service: String) {
        return totpSecretStorage.deleteSecret(service)
    }

    init {
        loadSecrets()
    }

    private fun loadSecrets() {
        _secrets.value = totpSecretStorage.getSecrets()
    }
}