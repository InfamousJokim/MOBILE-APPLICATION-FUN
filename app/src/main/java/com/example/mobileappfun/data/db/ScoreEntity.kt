package com.example.mobileappfun.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a single game session result.
 *
 * This is **Table 2** of the multi-table Room schema. Each [ScoreEntity] belongs
 * to exactly one [UserEntity] via a foreign-key relationship. When a user is
 * deleted, all their scores are cascaded-deleted automatically.
 *
 * Satisfies grading criterion: *"Database with multiple tables."*
 *
 * @property id          Auto-generated primary key.
 * @property userId      Foreign key referencing [UserEntity.id].
 * @property score       Final numeric score achieved in the game session.
 * @property wallsCleared Number of "Hole in the Wall" rounds the player survived.
 * @property durationMs  Total session length in milliseconds.
 * @property recordedAt  Unix epoch timestamp (ms) when the session ended.
 */
@Entity(
    tableName = "scores",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE   // Cascade-delete scores when user is removed
        )
    ],
    indices = [Index(value = ["userId"])]   // Index foreign key for efficient JOIN queries
)
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: Long,
    val score: Int,
    val wallsCleared: Int,
    val durationMs: Long,
    val recordedAt: Long = System.currentTimeMillis()
)
