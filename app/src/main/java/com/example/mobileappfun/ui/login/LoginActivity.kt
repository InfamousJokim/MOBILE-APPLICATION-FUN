package com.example.mobileappfun.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import com.example.mobileappfun.MainActivity
import com.example.mobileappfun.data.db.UserEntity

/**
 * Entry-point Activity for the application.
 *
 * Hosts the Jetpack Compose [LoginScreen] and acts as the coordinator between
 * the Compose UI layer and Android's Intent-based navigation system.
 *
 * **Flow:**
 * 1. App launches → [LoginActivity] is shown (declared as launcher in AndroidManifest).
 * 2. User logs in / registers → [LoginViewModel] emits [LoginUiState.authenticatedUser].
 * 3. [LoginScreen] invokes [onAuthSuccess] → [navigateToMain] fires.
 * 4. [MainActivity] starts with the authenticated [UserEntity] passed as an Extra.
 *
 * Declare in `AndroidManifest.xml` as:
 * ```xml
 * <activity android:name=".ui.login.LoginActivity"
 *     android:exported="true">
 *     <intent-filter>
 *         <action android:name="android.intent.action.MAIN"/>
 *         <category android:name="android.intent.category.LAUNCHER"/>
 *     </intent-filter>
 * </activity>
 * ```
 * And remove the `<intent-filter>` from [MainActivity].
 */
class LoginActivity : ComponentActivity() {

    /**
     * ViewModel scoped to this Activity's lifecycle.
     * [LoginViewModelFactory] provides the [UserRepository] dependency.
     */
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Apply a dark Material3 theme consistent with the game's aesthetic
            MaterialTheme(colorScheme = darkColorScheme()) {
                LoginScreen(
                    viewModel = viewModel,
                    onAuthSuccess = ::navigateToMain
                )
            }
        }
    }

    /**
     * Launches [MainActivity], forwarding the authenticated user's ID and username
     * so the game can personalise the experience and correctly attribute scores.
     *
     * Calls [finish] so the back button from [MainActivity] exits the app rather
     * than returning to the login screen.
     *
     * @param user The [UserEntity] returned by a successful login or registration.
     */
    private fun navigateToMain(user: UserEntity) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_USER_ID, user.id)
            putExtra(EXTRA_USERNAME, user.username)
        }
        startActivity(intent)
        finish() // Prevent back-navigation to the login screen after successful login
    }

    companion object {
        /** Intent Extra key for the authenticated user's database ID. */
        const val EXTRA_USER_ID = "extra_user_id"

        /** Intent Extra key for the authenticated user's display name. */
        const val EXTRA_USERNAME = "extra_username"
    }
}
