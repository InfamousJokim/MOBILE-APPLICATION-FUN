package com.example.mobileappfun.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a registered player in the local database.
 *
 * This is **Table 1** of the multi-table Room schema. A [UserEntity] is the parent
 * in a one-to-many relationship with [ScoreEntity] — each user can accumulate
 * many game scores over time.
 *
 * @property id         Auto-generated primary key (Room handles generation).
 * @property username   Unique display name chosen at registration. Indexed for
 *                      fast lookup during login.
 * @property passwordHash SHA-256 hash of the user's password. **Never store
 *                      plaintext passwords.**
 * @property avatarResId Optional drawable resource ID for the player avatar.
 * @property createdAt  Unix epoch timestamp (ms) of account creation.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val username: String,
    val passwordHash: String,
    val avatarResId: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
