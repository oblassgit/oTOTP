package org.olidev.ototp.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.olidev.ototp.Algorithm

@Entity(tableName = "totp_tokens")
data class TOTPTokenEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String?,
    val issuer: String,
    val algorithm: Algorithm,
    val digits: Int,
    val period: Int
)