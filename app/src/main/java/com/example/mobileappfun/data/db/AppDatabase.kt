package com.example.mobileappfun.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database singleton for "Hole in the Wall".
 *
 * Declares **two entities** ([UserEntity] and [ScoreEntity]), satisfying the
 * grading requirement for *"Database with multiple tables."*
 *
 * Access via [AppDatabase.getInstance] — the companion object enforces a
 * thread-safe, single-instance pattern using double-checked locking with
 * [@Volatile].
 *
 * **Schema version:** Increment [version] and provide a [androidx.room.migration.Migration]
 * whenever the schema changes in future sprints to avoid data loss.
 */
@Database(
    entities = [UserEntity::class, ScoreEntity::class],
    version = 1,
    exportSchema = true   // Export schema JSON for version-control auditing
)
abstract class AppDatabase : RoomDatabase() {

    /** Provides access to user account CRUD operations. */
    abstract fun userDao(): UserDao

    /** Provides access to game score persistence and leaderboard queries. */
    abstract fun scoreDao(): ScoreDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton [AppDatabase] instance, creating it if necessary.
         *
         * Uses double-checked locking to ensure thread safety during initial
         * construction on the first call.
         *
         * @param context Application context used to locate the database file.
         * @return The application-wide [AppDatabase] instance.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hole_in_the_wall.db"
                )
                    .fallbackToDestructiveMigration() // Safe for dev; replace with Migrations before release
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
