package com.example.ototp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "totp_tokens")
data class TOTPTokenEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String?,
    val issuer: String,
    val algorithm: Algorithm,
    val digits: Int?,
    val period: Int?
)