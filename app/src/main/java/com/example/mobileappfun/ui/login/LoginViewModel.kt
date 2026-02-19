package com.example.mobileappfun.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mobileappfun.data.AuthResult
import com.example.mobileappfun.data.UserRepository
import com.example.mobileappfun.data.db.AppDatabase
import com.example.mobileappfun.data.db.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Login/Register screen.
 *
 * Holds all UI state in a single [LoginUiState] wrapped in a [StateFlow], which
 * the Compose [LoginScreen] collects as state. Business logic (credential
 * validation, hashing, DB calls) is fully delegated to [UserRepository],
 * keeping this class focused purely on UI state management.
 *
 * Survives configuration changes (rotation), preventing re-login prompts.
 *
 * @property repository The data source for all auth operations.
 */
class LoginViewModel(private val repository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())

    /**
     * Immutable public [StateFlow] observed by [LoginScreen] via `collectAsState()`.
     * The Compose UI re-composes automatically whenever this emits a new value.
     */
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // ── Field Update Handlers ─────────────────────────────────────────────────

    /** Called on every keystroke in the username field. */
    fun onUsernameChange(value: String) =
        _uiState.update { it.copy(username = value, errorMessage = null) }

    /** Called on every keystroke in the password field. */
    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, errorMessage = null) }

    /** Toggles between Login and Register modes. */
    fun toggleMode() = _uiState.update {
        it.copy(isRegisterMode = !it.isRegisterMode, errorMessage = null)
    }

    // ── Auth Actions ──────────────────────────────────────────────────────────

    /**
     * Initiates the login flow. Validates inputs locally before hitting the DB.
     * On success, updates [LoginUiState.authenticatedUser] — the Compose screen
     * observes this and navigates to [MainActivity].
     */
    fun login() {
        val state = _uiState.value
        if (!validateInputs(state)) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = repository.login(state.username.trim(), state.password)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoading = false, authenticatedUser = result.user)
                }
                is AuthResult.InvalidCredentials -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Invalid username or password.")
                }
                is AuthResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Unexpected error.")
                }
            }
        }
    }

    /**
     * Initiates the registration flow. Validates inputs locally, then delegates
     * to [UserRepository.register]. On success, automatically logs the user in
     * by setting [LoginUiState.authenticatedUser].
     */
    fun register() {
        val state = _uiState.value
        if (!validateInputs(state)) return
        if (state.username.trim().length < 3) {
            _uiState.update { it.copy(errorMessage = "Username must be at least 3 characters.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = repository.register(state.username.trim(), state.password)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoading = false, authenticatedUser = result.user)
                }
                is AuthResult.UsernameTaken -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Username already taken. Try another.")
                }
                is AuthResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Unexpected error.")
                }
            }
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Validates that username and password fields are non-empty and the password
     * meets a minimum length requirement. Updates [LoginUiState.errorMessage]
     * on failure and returns false to abort the auth call.
     *
     * @param state Current UI state snapshot.
     * @return true if inputs are valid, false otherwise.
     */
    private fun validateInputs(state: LoginUiState): Boolean {
        return when {
            state.username.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Username cannot be empty.") }
                false
            }
            state.password.length < 6 -> {
                _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
                false
            }
            else -> true
        }
    }
}

// ── UI State ──────────────────────────────────────────────────────────────────

/**
 * Immutable snapshot of the Login screen's UI state.
 *
 * Compose observes this via [StateFlow.collectAsState] and re-composes only the
 * affected parts of the UI when a field changes (structural equality via `data class`).
 *
 * @property username          Current value of the username text field.
 * @property password          Current value of the password text field.
 * @property isLoading         True while an async DB operation is in progress —
 *                             disables buttons and shows a [CircularProgressIndicator].
 * @property isRegisterMode    True when the form is in "Create Account" mode.
 * @property errorMessage      Non-null string to display inside a red error banner.
 * @property authenticatedUser Non-null after a successful auth — triggers navigation
 *                             to [MainActivity].
 */
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isRegisterMode: Boolean = false,
    val errorMessage: String? = null,
    val authenticatedUser: UserEntity? = null
)

// ── Factory ───────────────────────────────────────────────────────────────────

/**
 * [ViewModelProvider.Factory] for [LoginViewModel].
 *
 * Required because [LoginViewModel] takes constructor parameters. The factory
 * manually instantiates [AppDatabase] and [UserRepository] — replace this with
 * a Hilt injection in a future sprint for cleaner dependency management.
 *
 * @param context Application context used to build [AppDatabase].
 */
class LoginViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.getInstance(context)
        val repo = UserRepository(db)
        return LoginViewModel(repo) as T
    }
}
