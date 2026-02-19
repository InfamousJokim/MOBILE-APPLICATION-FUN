package com.example.mobileappfun.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for [ScoreEntity].
 *
 * Provides reactive leaderboard queries and score insertion. All queries that
 * return a [Flow] will automatically push updates to observers when the scores
 * table changes — ideal for a live leaderboard screen.
 */
@Dao
interface ScoreDao {

    /**
     * Persists a completed game session's result.
     *
     * @param score The [ScoreEntity] to insert.
     * @return The auto-generated row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: ScoreEntity): Long

    /**
     * Retrieves all scores for a specific user, ordered by score descending.
     * Powers the "Personal Best" screen.
     *
     * @param userId The [UserEntity.id] whose scores to fetch.
     * @return A [Flow] that emits the user's score history.
     */
    @Query("SELECT * FROM scores WHERE userId = :userId ORDER BY score DESC")
    fun getScoresForUser(userId: Long): Flow<List<ScoreEntity>>

    /**
     * Retrieves the global top-N scores joined with usernames for the leaderboard.
     * Uses a JOIN across both tables — demonstrating multi-table relational queries.
     *
     * @param limit Maximum number of entries to return.
     * @return A [Flow] of [LeaderboardEntry] projections.
     */
    @Query("""
        SELECT u.username, s.score, s.wallsCleared, s.recordedAt
        FROM scores s
        INNER JOIN users u ON s.userId = u.id
        ORDER BY s.score DESC
        LIMIT :limit
    """)
    fun getLeaderboard(limit: Int = 10): Flow<List<LeaderboardEntry>>

    /**
     * Returns the single highest score ever achieved by a specific user.
     *
     * @param userId The user's ID.
     * @return The personal best score, or null if the user has no recorded games.
     */
    @Query("SELECT MAX(score) FROM scores WHERE userId = :userId")
    suspend fun getPersonalBest(userId: Long): Int?
}

/**
 * Projection data class used by the leaderboard JOIN query.
 * Room maps the query columns directly onto this class without needing a full Entity.
 *
 * @property username     Player's display name.
 * @property score        Achieved score.
 * @property wallsCleared Number of rounds survived.
 * @property recordedAt   Timestamp of the session.
 */
data class LeaderboardEntry(
    val username: String,
    val score: Int,
    val wallsCleared: Int,
    val recordedAt: Long
)
