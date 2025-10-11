package com.example.ototp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TokenRepository(
    private val dao: TokenDao,
    private val secretStorage: TotpSecretStorage
) {
    suspend fun addToken(token: TOTPToken): Long {
        val entity = TOTPTokenEntity(
            label = token.label,
            issuer = token.issuer,
            algorithm = token.algorithm,
            digits = token.digits,
            period = token.period
        )
        val id = dao.insert(entity)
        secretStorage.saveSecret(id, token.secret)
        return id
    }

    suspend fun updateToken(token: TOTPToken) {
        require(token.id != null) { "Token ID required for update" }
        val entity = TOTPTokenEntity(
            id = token.id,
            label = token.label,
            issuer = token.issuer,
            algorithm = token.algorithm,
            digits = token.digits,
            period = token.period
        )
        dao.update(entity)
        secretStorage.saveSecret(token.id, token.secret)
    }

    suspend fun deleteToken(tokenId: Long) {
        dao.deleteById(tokenId)
        secretStorage.deleteSecret(tokenId)
    }

    suspend fun getAllTokens(): List<TOTPTokenEntity> = withContext(Dispatchers.IO) {
        dao.getAllTokens()
    }

    fun getSecret(tokenId: Long): String? = secretStorage.getSecret(tokenId)

}