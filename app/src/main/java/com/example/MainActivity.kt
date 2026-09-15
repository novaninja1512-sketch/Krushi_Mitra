package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.auth.AuthRepository
import com.example.data.database.AppDatabase
import com.example.data.repository.FarmRepository
import com.example.data.sync.SyncEngine
import com.example.ui.KrushiApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.FarmViewModel
import com.example.viewmodel.FarmViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var authRepository: AuthRepository
    private lateinit var syncEngine: SyncEngine

    private val viewModel: FarmViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        authRepository = AuthRepository(applicationContext)
        syncEngine = SyncEngine(applicationContext, database, authRepository)
        val repository = FarmRepository(database, syncEngine, authRepository)
        FarmViewModelFactory(repository, authRepository, syncEngine)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleAuthIntent(intent)

        setContent {
            MyApplicationTheme {
                KrushiApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent?) {
        if (intent == null || intent.data == null) return
        if (::authRepository.isInitialized && ::syncEngine.isInitialized) {
            authRepository.handleAuthCallback(intent) { userId ->
                syncEngine.performSync()
            }
        }
    }
}


