package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.database.AppDatabase
import com.example.data.repository.FarmRepository
import com.example.ui.KrushiApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.FarmViewModel
import com.example.viewmodel.FarmViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: FarmViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = FarmRepository(database)
        FarmViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                KrushiApp(viewModel = viewModel)
            }
        }
    }
}

