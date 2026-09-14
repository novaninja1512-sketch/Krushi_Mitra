package com.example.data.sync

import android.util.Log
import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClientProvider {
    private const val TAG = "SupabaseClientProvider"

    private var _client: SupabaseClient? = null

    val isConfigured: Boolean
        get() {
            val url = getSupabaseUrl()
            val key = getSupabaseAnonKey()
            return url.isNotBlank() &&
                    !url.contains("placeholder") &&
                    !url.contains("your-project") &&
                    key.isNotBlank() &&
                    !key.contains("placeholder") &&
                    !key.contains("dummy")
        }

    fun getSupabaseUrl(): String {
        return try {
            BuildConfig.SUPABASE_URL.trim()
        } catch (e: Exception) {
            ""
        }
    }

    fun getSupabaseAnonKey(): String {
        return try {
            BuildConfig.SUPABASE_ANON_KEY.trim()
        } catch (e: Exception) {
            ""
        }
    }

    fun getClient(): SupabaseClient? {
        if (_client != null) return _client

        val url = getSupabaseUrl()
        val key = getSupabaseAnonKey()

        if (!isConfigured) {
            Log.w(TAG, "Supabase credentials not configured in BuildConfig / .env. Running in local-only mode.")
            return null
        }

        return try {
            val client = createSupabaseClient(
                supabaseUrl = url,
                supabaseKey = key
            ) {
                install(Auth) {
                    scheme = "krushimitra"
                    host = "auth"
                    alwaysAutoRefresh = true
                    autoLoadFromStorage = true
                }
                install(Postgrest)
            }
            _client = client
            client
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Supabase client: ${e.message}", e)
            null
        }
    }
}
