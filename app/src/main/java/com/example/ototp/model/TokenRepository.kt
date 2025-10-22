package com.example.ototp.model

import com.example.ototp.TOTPToken
import com.example.ototp.db.TOTPTokenEntity
import com.example.ototp.db.TokenDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TokenRepository(
    private val dao: TokenDao,
    private val secretStorage: TotpSecretStorage
) {
    suspend fun addToken(token: TOTPToken): Long {
        val entity = TOTPTokenEntity(
            label = token.account,
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
            label = token.account,
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

    fun getToken(id: Long): Flow<TOTPTokenEntity> = dao.getTokenFlow(id)

    fun getSecret(tokenId: Long): String? = secretStorage.getSecret(tokenId)

}