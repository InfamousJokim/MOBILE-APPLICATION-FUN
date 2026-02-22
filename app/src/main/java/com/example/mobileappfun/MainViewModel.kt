package com.example.mobileappfun

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mobileappfun.data.UserRepository
import com.example.mobileappfun.data.db.AppDatabase
import com.example.mobileappfun.data.db.ScoreEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Activity-scoped ViewModel that holds the authenticated user's session state.
 * All fragments access this via activityViewModels() so they share the same instance.
 */
class MainViewModel(private val repository: UserRepository) : ViewModel() {

    private val _userId = MutableStateFlow(-1L)
    val userId: StateFlow<Long> = _userId

    private val _username = MutableStateFlow("Player")
    val username: StateFlow<String> = _username

    private val _personalBest = MutableStateFlow(0)
    val personalBest: StateFlow<Int> = _personalBest

    fun setUser(userId: Long, username: String) {
        _userId.value = userId
        _username.value = username
        refreshPersonalBest()
    }

    fun refreshPersonalBest() {
        val uid = _userId.value
        if (uid == -1L) return
        viewModelScope.launch {
            _personalBest.value = repository.getPersonalBest(uid) ?: 0
        }
    }

    fun saveGameScore(score: Int, correctCount: Int) {
        val uid = _userId.value
        if (uid == -1L) return
        viewModelScope.launch {
            repository.saveScore(
                ScoreEntity(
                    userId = uid,
                    score = score,
                    wallsCleared = correctCount,
                    durationMs = 0L
                )
            )
            refreshPersonalBest()
        }
    }
}

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.getInstance(context)
        val repo = UserRepository(db)
        return MainViewModel(repo) as T
    }
}
