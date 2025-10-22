package com.example.ototp.activity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ototp.TOTPToken
import com.example.ototp.db.TOTPTokenEntity
import com.example.ototp.model.TokenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MyViewModel(
    private val repository: TokenRepository
) : ViewModel() {

    private val _tokens = MutableStateFlow<List<TOTPTokenEntity>>(emptyList())
    val tokens: StateFlow<List<TOTPTokenEntity>> = _tokens

    var tokenDraft: TOTPToken? by mutableStateOf(null)

    init {
        refreshTokens()
    }

    fun refreshTokens() {
        viewModelScope.launch {
            _tokens.value = repository.getAllTokens()
        }
    }

    fun addToken(token: TOTPToken) {
        viewModelScope.launch {
            repository.addToken(token)
            refreshTokens()
        }
    }

    fun updateToken(token: TOTPToken) {
        viewModelScope.launch {
            repository.updateToken(token)
            refreshTokens()
        }
    }

    fun deleteToken(tokenId: Long) {
        viewModelScope.launch {
            repository.deleteToken(tokenId)
            refreshTokens()
        }
    }

    fun getToken(tokenId: Long): Flow<TOTPTokenEntity?> = repository.getToken(tokenId)

    fun getSecret(tokenId: Long): String? = repository.getSecret(tokenId)
}