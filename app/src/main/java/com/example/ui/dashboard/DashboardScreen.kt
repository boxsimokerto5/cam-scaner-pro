package com.example.ui.dashboard

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DocumentEntity
import com.example.data.repository.DocumentRepository
import com.example.scanner.DocumentScannerHelper
import com.example.security.BiometricAuthManager
import com.example.tools.PdfToolsManager
import com.example.ui.components.*
import com.example.ui.navigation.BottomNavItem
import com.example.ui.screens.*
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    biometricManager: BiometricAuthManager,
    toolsManager: PdfToolsManager,
    repository: DocumentRepository
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Active bottom navigation tab
    var currentTab by remember { mutableStateOf(BottomNavItem.HOME) }
    var initialToolsSubTab by remember { mutableIntStateOf(0) }

    // Back handler: return to HOME tab if on another sub-screen
    BackHandler(enabled = currentTab != BottomNavItem.HOME) {
        currentTab = BottomNavItem.HOME
    }

    // Active dialog states
    var previewDocument by remember { mutableStateOf<DocumentEntity?>(null) }
    var ocrDocument by remember { mutableStateOf<DocumentEntity?>(null) }
    var editDocument by remember { mutableStateOf<DocumentEntity?>(null) }

    // ML Kit Document Scanner Intent Launcher
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            if (scanResult != null) {
                viewModel.onScanResultReceived(scanResult)
            }
        }
    }

    // Modern Zero-Permission Photo Picker for Gallery Import
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onGalleryImagesReceived(uris)
        }
    }

    // File Picker for "Impor File"
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onGalleryImagesReceived(uris)
        }
    }

    val scannerHelper = remember(activity) {
        activity?.let { DocumentScannerHelper(it) }
    }

    val launchScanner = {
        scannerHelper?.startScan(
            onIntentSenderReady = { request -> scannerLauncher.launch(request) },
            onError = { err ->
                Toast.makeText(context, "Kamera pemindai: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        ) ?: Toast.makeText(context, "Scanner tidak siap", Toast.LENGTH_SHORT).show()
    }

    // Display status messages via Snackbar
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // Share Helper
    val sharePdf = { doc: DocumentEntity ->
        try {
            val shareUri = viewModel.getShareablePdfUri(doc.pdfPath)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_SUBJECT, doc.title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Dokumen"))
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Biometric Unlock Trigger
    val triggerBiometricUnlock = {
        if (biometricManager.isBiometricAvailable()) {
            biometricManager.promptBiometric(
                title = "Buka Kunci Dokumen",
                subtitle = "Verifikasi identitas untuk melihat dokumen",
                onSuccess = { viewModel.unlockApp() },
                onError = { /* ignored */ }
            )
        } else {
            viewModel.unlockApp()
        }
    }

    // Lock Barrier Screen
    if (uiState.isAppLocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE0F2FE),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = Color(0xFFE6F7F2)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = Color(0xFF00A884)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Aplikasi Terkunci",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Dokumen Anda tersimpan 100% aman dan privat di perangkat ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = triggerBiometricUnlock,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("unlock_app_button")
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buka Kunci")
                    }
                }
            }
        }
        return
    }

    // CamScanner Main Scaffold
    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // 4 Items Bottom Navigation matching CamScanner screenshot
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(0.6.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        val activeColor = Color(0xFF00A884) // CamScanner Emerald Teal
                        val inactiveColor = Color(0xFF94A3B8)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { currentTab = tab }
                                .padding(vertical = 4.dp)
                                .testTag(tab.testTag)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) activeColor else inactiveColor,
                                    modifier = Modifier.size(24.dp)
                                )

                                // Red "EDU" badge on "Saya"
                                if (tab == BottomNavItem.PROFILE) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFEF4444),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 10.dp, y = (-4).dp)
                                    ) {
                                        Text(
                                            text = "EDU",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = tab.title,
                                color = if (isSelected) activeColor else inactiveColor,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            // Large circular camera FAB matching CamScanner bottom-right button
            if (currentTab == BottomNavItem.HOME || currentTab == BottomNavItem.DOCUMENTS) {
                FloatingActionButton(
                    onClick = launchScanner,
                    containerColor = Color(0xFF00A884),
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .size(60.dp)
                        .testTag("camscanner_fab_camera")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Pindai Dokumen",
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                BottomNavItem.HOME -> {
                    // Exact Home Layout from user's screenshot
                    CamScannerHomeScreen(
                        documents = uiState.documents,
                        searchQuery = uiState.searchQuery,
                        onSearchChange = viewModel::onSearchQueryChanged,
                        onScanClick = launchScanner,
                        onPdfToolsClick = {
                            initialToolsSubTab = 0
                            currentTab = BottomNavItem.TOOLS
                        },
                        onImportImageClick = {
                            galleryPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onImportFileClick = {
                            filePickerLauncher.launch(arrayOf("application/pdf", "image/*"))
                        },
                        onIdCardClick = {
                            Toast.makeText(context, "Mode Kartu ID Siap. Membuka kamera...", Toast.LENGTH_SHORT).show()
                            launchScanner()
                        },
                        onSignClick = {
                            initialToolsSubTab = 2 // Edit PDF / Watermark / Tanda tangan
                            currentTab = BottomNavItem.TOOLS
                        },
                        onQrScanClick = {
                            Toast.makeText(context, "Membuka scanner kode QR & dokumen...", Toast.LENGTH_SHORT).show()
                            launchScanner()
                        },
                        onAllToolsClick = {
                            initialToolsSubTab = 0
                            currentTab = BottomNavItem.TOOLS
                        },
                        onViewAllDocumentsClick = {
                            currentTab = BottomNavItem.DOCUMENTS
                        },
                        onDocumentClick = { doc -> previewDocument = doc },
                        onShareClick = { doc -> sharePdf(doc) },
                        onOcrClick = { doc -> ocrDocument = doc }
                    )
                }

                BottomNavItem.DOCUMENTS -> {
                    // "Semua Doku..." full archive tab
                    AllDocumentsScreen(
                        uiState = uiState,
                        onSearchChanged = viewModel::onSearchQueryChanged,
                        onCategorySelected = { cat -> viewModel.onCategorySelected(cat ?: "Semua") },
                        onDocumentClick = { doc -> previewDocument = doc },
                        onShareClick = { doc -> sharePdf(doc) },
                        onOcrClick = { doc -> ocrDocument = doc },
                        onEditClick = { doc -> editDocument = doc },
                        onDeleteClick = { doc -> viewModel.deleteDocument(doc) },
                        onScanClick = launchScanner
                    )
                }

                BottomNavItem.TOOLS -> {
                    // "Alat" Complete Toolkit matching CamScanner
                    ToolsTabScreen(
                        repository = repository,
                        toolsManager = toolsManager,
                        documents = uiState.documents,
                        onScanClick = launchScanner,
                        onImportImageClick = {
                            galleryPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onImportFileClick = {
                            filePickerLauncher.launch(arrayOf("application/pdf", "image/*"))
                        },
                        onLockAppClick = { viewModel.lockApp() },
                        onDocumentCreated = { /* Room flow updates automatically */ }
                    )
                }

                BottomNavItem.PROFILE -> {
                    // "Saya" Profile & Settings
                    ProfileScreen(
                        documents = uiState.documents,
                        biometricManager = biometricManager,
                        onLockApp = { viewModel.lockApp() }
                    )
                }
            }

            // Global Loading Indicator
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFF00A884))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = uiState.loadingMessage.ifBlank { "Memproses dokumen..." },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    previewDocument?.let { doc ->
        PdfPreviewDialog(
            document = doc,
            onDismiss = { previewDocument = null },
            onShare = { sharePdf(doc) },
            onViewOcr = {
                previewDocument = null
                ocrDocument = doc
            }
        )
    }

    ocrDocument?.let { doc ->
        OcrTextDialog(
            document = doc,
            onDismiss = { ocrDocument = null }
        )
    }

    editDocument?.let { doc ->
        EditDocumentDialog(
            document = doc,
            onDismiss = { editDocument = null },
            onSave = { newTitle, newCategory ->
                viewModel.updateDocument(doc, newTitle, newCategory)
                editDocument = null
            }
        )
    }

    // Save Scan Dialog for Camera Scans
    uiState.pendingScanResult?.let { scan ->
        SaveScanDialog(
            pageCount = scan.pages?.size ?: 1,
            isFromGallery = false,
            onDismiss = { viewModel.dismissPendingDialogs() },
            onConfirm = { title, category ->
                viewModel.confirmSaveScan(title, category)
            }
        )
    }

    // Save Scan Dialog for Gallery Imports
    uiState.pendingGalleryUris?.let { uris ->
        SaveScanDialog(
            pageCount = uris.size,
            isFromGallery = true,
            onDismiss = { viewModel.dismissPendingDialogs() },
            onConfirm = { title, category ->
                viewModel.confirmSaveGallery(title, category)
            }
        )
    }
}
