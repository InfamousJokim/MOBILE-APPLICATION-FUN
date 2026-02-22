package com.example.mobileappfun.data

import com.example.mobileappfun.data.db.AppDatabase
import com.example.mobileappfun.data.db.ScoreEntity
import com.example.mobileappfun.data.db.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Repository that abstracts all user-account and score data operations.
 *
 * The [LoginViewModel] and future game-result ViewModels interact **only** with
 * this class — never directly with the DAOs — enforcing the single-source-of-truth
 * pattern and making the data layer testable.
 *
 * All suspend functions switch to [Dispatchers.IO] internally so callers on the
 * main thread (e.g., `viewModelScope.launch`) are safe.
 *
 * @property db The [AppDatabase] instance injected from [LoginViewModelFactory].
 */
class UserRepository(private val db: AppDatabase) {

    // ── Auth ──────────────────────────────────────────────────────────────────

    /**
     * Attempts to register a new user account.
     *
     * Hashes the password with SHA-256 before persistence — plaintext passwords
     * are never written to disk.
     *
     * @param username Desired username (must be unique).
     * @param password Plaintext password provided by the user.
     * @return [AuthResult.Success] with the new [UserEntity], or
     *         [AuthResult.UsernameTaken] / [AuthResult.Error] on failure.
     */
    suspend fun register(username: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                val existing = db.userDao().findByUsername(username)
                if (existing != null) return@withContext AuthResult.UsernameTaken

                val hash = sha256(password)
                val id = db.userDao().insertUser(
                    UserEntity(username = username, passwordHash = hash)
                )
                val user = db.userDao().findByCredentials(username, hash)
                    ?: return@withContext AuthResult.Error("Registration failed")
                AuthResult.Success(user)
            } catch (e: Exception) {
                AuthResult.Error(e.message ?: "Unknown error")
            }
        }

    /**
     * Validates login credentials against the stored password hash.
     *
     * @param username Username entered by the player.
     * @param password Plaintext password entered by the player.
     * @return [AuthResult.Success] with the authenticated [UserEntity], or
     *         [AuthResult.InvalidCredentials] if no match is found.
     */
    suspend fun login(username: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val hash = sha256(password)
            val user = db.userDao().findByCredentials(username, hash)
            if (user != null) AuthResult.Success(user)
            else AuthResult.InvalidCredentials
        }

    // ── Scores ────────────────────────────────────────────────────────────────

    /**
     * Saves the result of a completed game session for the logged-in user.
     *
     * @param score The [ScoreEntity] to persist. Must have a valid [ScoreEntity.userId].
     */
    suspend fun saveScore(score: ScoreEntity) = withContext(Dispatchers.IO) {
        db.scoreDao().insertScore(score)
    }

    /**
     * Returns a reactive [Flow] of the global leaderboard (top 10).
     * Automatically re-emits when the scores table changes.
     */
    fun getLeaderboard() = db.scoreDao().getLeaderboard(limit = 10)

    /**
     * Returns a reactive [Flow] of scores for the specified user.
     */
    fun getScoresForUser(userId: Long): Flow<List<ScoreEntity>> =
        db.scoreDao().getScoresForUser(userId)

    /**
     * Returns the highest score ever achieved by a specific user.
     */
    suspend fun getPersonalBest(userId: Long): Int? = withContext(Dispatchers.IO) {
        db.scoreDao().getPersonalBest(userId)
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    /**
     * Computes the SHA-256 hash of a given plaintext string and returns it as
     * a lowercase hex string.
     *
     * @param input The plaintext to hash (e.g., a raw password).
     * @return 64-character lowercase hex digest.
     */
    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}

// ── Result Sealed Class ───────────────────────────────────────────────────────

/**
 * Sealed class representing all possible outcomes of an authentication operation.
 * The ViewModel maps these to UI states consumed by the Compose login screen.
 */
sealed class AuthResult {
    /** Authentication succeeded. [user] is the authenticated [UserEntity]. */
    data class Success(val user: UserEntity) : AuthResult()

    /** Registration failed — the chosen username is already registered. */
    object UsernameTaken : AuthResult()

    /** Login failed — no account matches the supplied credentials. */
    object InvalidCredentials : AuthResult()

    /** An unexpected error occurred. [message] contains diagnostic detail. */
    data class Error(val message: String) : AuthResult()
}
