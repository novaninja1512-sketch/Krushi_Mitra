package com.example.data.auth

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.example.data.sync.SupabaseClientProvider
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UserProfile(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null
)

sealed interface AuthState {
    object Loading : AuthState
    data class Authenticated(val user: UserProfile) : AuthState
    object Unauthenticated : AuthState
    data class Error(val message: String) : AuthState
}

class AuthRepository(private val context: Context) {
    private val TAG = "AuthRepository"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val prefs: SharedPreferences = context.getSharedPreferences("krushi_auth_prefs", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    init {
        initializeAuth()
    }

    private fun initializeAuth() {
        // First check cached user to prevent flashing login screen
        val cachedId = prefs.getString("user_id", null)
        val cachedEmail = prefs.getString("user_email", null)
        val cachedName = prefs.getString("user_name", null)
        val cachedAvatar = prefs.getString("user_avatar", null)

        if (!cachedId.isNullOrBlank() && !cachedEmail.isNullOrBlank()) {
            val profile = UserProfile(
                id = cachedId,
                email = cachedEmail,
                displayName = cachedName ?: cachedEmail.substringBefore("@"),
                avatarUrl = cachedAvatar
            )
            _currentUser.value = profile
            _authState.value = AuthState.Authenticated(profile)
        } else {
            _authState.value = AuthState.Unauthenticated
        }

        val client = SupabaseClientProvider.getClient() ?: return

        scope.launch {
            try {
                client.auth.sessionStatus.collect { status ->
                    when (status) {
                        is SessionStatus.Authenticated -> {
                            val user = client.auth.currentUserOrNull()
                            if (user != null) {
                                val meta = user.userMetadata
                                val name = meta?.get("full_name")?.toString()?.trim('"')
                                    ?: meta?.get("name")?.toString()?.trim('"')
                                    ?: user.email?.substringBefore("@")
                                    ?: "Farmer"
                                val avatar = meta?.get("avatar_url")?.toString()?.trim('"')
                                val profile = UserProfile(
                                    id = user.id,
                                    email = user.email ?: "",
                                    displayName = name,
                                    avatarUrl = avatar
                                )
                                saveUserToPrefs(profile)
                                _currentUser.value = profile
                                _authState.value = AuthState.Authenticated(profile)
                            }
                        }
                        is SessionStatus.NotAuthenticated -> {
                            // If user is explicitly not authenticated in Supabase, clear local session
                            if (SupabaseClientProvider.isConfigured) {
                                clearPrefs()
                                _currentUser.value = null
                                _authState.value = AuthState.Unauthenticated
                            }
                        }
                        is SessionStatus.Initializing -> {
                            // Keep current loading or cached state
                        }
                        else -> {
                            // RefreshFailure or other statuses
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error monitoring auth session: ${e.message}", e)
            }
        }
    }

    suspend fun signInWithGoogle(): Result<Unit> = withContext(Dispatchers.IO) {
        val client = SupabaseClientProvider.getClient()
            ?: return@withContext Result.failure(IllegalStateException("Supabase is not configured. Please check .env settings."))

        return@withContext try {
            client.auth.signInWith(Google)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in with Google failed: ${e.message}", e)
            _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            Result.failure(e)
        }
    }

    fun handleAuthCallback(intent: Intent, onAuthSuccess: suspend (userId: String) -> Unit) {
        val client = SupabaseClientProvider.getClient() ?: return
        try {
            client.handleDeeplinks(intent) {
                // Callback handled
                val user = client.auth.currentUserOrNull()
                if (user != null) {
                    val meta = user.userMetadata
                    val name = meta?.get("full_name")?.toString()?.trim('"')
                        ?: meta?.get("name")?.toString()?.trim('"')
                        ?: user.email?.substringBefore("@")
                        ?: "Farmer"
                    val avatar = meta?.get("avatar_url")?.toString()?.trim('"')
                    val profile = UserProfile(
                        id = user.id,
                        email = user.email ?: "",
                        displayName = name,
                        avatarUrl = avatar
                    )
                    saveUserToPrefs(profile)
                    _currentUser.value = profile
                    _authState.value = AuthState.Authenticated(profile)
                    scope.launch {
                        onAuthSuccess(user.id)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling auth deeplink: ${e.message}", e)
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val client = SupabaseClientProvider.getClient()
            client?.auth?.signOut()
            clearPrefs()
            _currentUser.value = null
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error: ${e.message}", e)
            clearPrefs()
            _currentUser.value = null
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        }
    }

    private fun saveUserToPrefs(profile: UserProfile) {
        prefs.edit()
            .putString("user_id", profile.id)
            .putString("user_email", profile.email)
            .putString("user_name", profile.displayName)
            .putString("user_avatar", profile.avatarUrl)
            .apply()
    }

    private fun clearPrefs() {
        prefs.edit().clear().apply()
    }
}
