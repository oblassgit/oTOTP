package com.example.ototp

import androidx.room.*

@Dao
interface TokenDao {
    @Insert
    suspend fun insert(token: TOTPTokenEntity): Long

    @Update
    suspend fun update(token: TOTPTokenEntity)

    @Query("DELETE FROM totp_tokens WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM totp_tokens")
    suspend fun getAllTokens(): List<TOTPTokenEntity>
}