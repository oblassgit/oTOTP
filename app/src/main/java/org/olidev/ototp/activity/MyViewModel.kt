package org.olidev.ototp.activity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.olidev.ototp.TOTPToken
import org.olidev.ototp.db.TOTPTokenEntity
import org.olidev.ototp.model.TokenRepository

class MyViewModel(
    private val repository: TokenRepository
) : ViewModel() {

    private val _tokens = MutableStateFlow<List<TOTPTokenEntity>>(emptyList())
    val tokens: StateFlow<List<TOTPTokenEntity>> = _tokens

    private val _secrets = MutableStateFlow<Map<Long, String>>(emptyMap())
    val secrets: StateFlow<Map<Long, String>> = _secrets

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val filteredTokens: StateFlow<List<TOTPTokenEntity>> = combine(_tokens, _searchQuery) { tokens, query ->
        if (query.isBlank()) tokens
        else tokens.filter { token ->
            token.issuer.contains(query, ignoreCase = true) ||
                token.label?.contains(query, ignoreCase = true) == true
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    var tokenDraft: TOTPToken? by mutableStateOf(null)

    init {
        refreshTokens()
    }

    fun refreshTokens() {
        viewModelScope.launch {
            val tokens = repository.getAllTokens()
            val secrets = withContext(Dispatchers.IO) {
                tokens.mapNotNull { token ->
                    repository.getSecret(token.id)?.let { token.id to it }
                }.toMap()
            }
            _tokens.value = tokens
            _secrets.value = secrets
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