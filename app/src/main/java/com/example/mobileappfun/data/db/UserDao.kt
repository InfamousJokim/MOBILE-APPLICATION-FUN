package com.example.mobileappfun.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for [UserEntity].
 *
 * All suspend functions are safe to call from a coroutine scope (e.g., inside
 * a [androidx.lifecycle.ViewModel] using `viewModelScope`). The [Flow]-returning
 * queries automatically re-emit when the underlying table changes, enabling
 * reactive UI updates through the MVVM pipeline.
 */
@Dao
interface UserDao {

    /**
     * Inserts a new user into the database.
     * [OnConflictStrategy.ABORT] causes an [android.database.sqlite.SQLiteConstraintException]
     * if the username already exists (enforced by the UNIQUE index), which the
     * ViewModel catches and surfaces as a "Username taken" error.
     *
     * @param user The [UserEntity] to persist.
     * @return The auto-generated row ID of the newly inserted user.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    /**
     * Looks up a user by username and password hash for login validation.
     * Returns null if no matching record exists (wrong credentials).
     *
     * @param username     The username entered by the player.
     * @param passwordHash SHA-256 hash of the entered password.
     * @return The matching [UserEntity] or null.
     */
    @Query("SELECT * FROM users WHERE username = :username AND passwordHash = :passwordHash LIMIT 1")
    suspend fun findByCredentials(username: String, passwordHash: String): UserEntity?

    /**
     * Checks whether a username is already registered.
     * Used during registration to provide immediate feedback.
     *
     * @param username The username to check.
     * @return The [UserEntity] if taken, null if available.
     */
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    /**
     * Emits the full list of registered users as a reactive [Flow].
     * Primarily used in an admin/leaderboard context.
     */
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>
}
