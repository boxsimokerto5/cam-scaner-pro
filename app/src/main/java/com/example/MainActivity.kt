package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.DocumentRepository
import com.example.data.storage.FileStorageManager
import com.example.scanner.OcrManager
import com.example.security.BiometricAuthManager
import com.example.tools.PdfToolsManager
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.dashboard.DashboardViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var storageManager: FileStorageManager
    private lateinit var toolsManager: PdfToolsManager
    private lateinit var ocrManager: OcrManager
    private lateinit var repository: DocumentRepository
    private lateinit var biometricManager: BiometricAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = AppDatabase.getDatabase(applicationContext)
        storageManager = FileStorageManager(applicationContext)
        toolsManager = PdfToolsManager(applicationContext)
        ocrManager = OcrManager(applicationContext)
        repository = DocumentRepository(
            documentDao = database.documentDao(),
            historyDao = database.historyDao(),
            storageManager = storageManager,
            ocrManager = ocrManager
        )
        biometricManager = BiometricAuthManager(this)

        setContent {
            MyApplicationTheme {
                val viewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModelFactory(
                        repository = repository,
                        initialLocked = false
                    )
                )

                DashboardScreen(
                    viewModel = viewModel,
                    biometricManager = biometricManager,
                    toolsManager = toolsManager,
                    repository = repository
                )
            }
        }
    }
}
