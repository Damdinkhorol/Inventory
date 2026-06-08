package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.InventoryRepository
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.InventoryApp
import com.example.ui.InventoryViewModel
import com.example.ui.InventoryViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val repository by lazy { InventoryRepository(database.inventoryDao()) }
    private val viewModel by lazy {
        ViewModelProvider(
            this,
            InventoryViewModelFactory(repository)
        )[InventoryViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.initializeDatabase(applicationContext)
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = isDarkTheme) {
                InventoryApp(viewModel = viewModel)
            }
        }
    }
}
