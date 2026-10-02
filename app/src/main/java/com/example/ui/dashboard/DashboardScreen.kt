package com.example.ui.dashboard

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    var currentTab by remember { mutableStateOf(BottomNavItem.DOCUMENTS) }

    // Back handler: return to Documents tab if on another tool tab
    BackHandler(enabled = currentTab != BottomNavItem.DOCUMENTS) {
        currentTab = BottomNavItem.DOCUMENTS
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

    val scannerHelper = remember(activity) {
        activity?.let { DocumentScannerHelper(it) }
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
            // handle error
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
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
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

    val bottomNavScrollState = rememberScrollState()

    // Main App Scaffold with Horizontally Scrollable Bottom Menu
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(bottomNavScrollState)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomNavItem.values().forEach { tab ->
                            val selected = currentTab == tab

                            val animatedBgColor by animateColorAsState(
                                targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                animationSpec = tween(durationMillis = 200),
                                label = "nav_bg"
                            )
                            val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = animatedBgColor,
                                border = if (selected) null else BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { currentTab = tab }
                                    .testTag(tab.testTag)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = contentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = tab.title,
                                        color = contentColor,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            // Only show Scanner FABs on the Documents Home tab
            if (currentTab == BottomNavItem.DOCUMENTS) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Gallery Import Small FAB
                    SmallFloatingActionButton(
                        onClick = {
                            galleryPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(14.dp),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                        modifier = Modifier.testTag("gallery_fab")
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Impor Galeri",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Main Scan Camera Extended FAB
                    ExtendedFloatingActionButton(
                        onClick = {
                            scannerHelper?.startScan(
                                onIntentSenderReady = { request -> scannerLauncher.launch(request) },
                                onError = { /* fallback */ }
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.DocumentScanner,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        text = {
                            Text(
                                "Pindai Dokumen",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                        modifier = Modifier.testTag("scan_camera_fab")
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
                BottomNavItem.DOCUMENTS -> {
                    // --- TAB 1: DOKUMEN (HOME) ---
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header Bar
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(
                                            painter = painterResource(id = com.example.R.drawable.app_launcher_icon_1790933447784),
                                            contentDescription = "Logo",
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "DocScanner",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(6.dp)
                                                ) {}
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "100% Offline • ${uiState.documents.size} Dokumen",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.lockApp() },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .size(40.dp)
                                            .testTag("lock_app_icon_button")
                                    ) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "Kunci Aplikasi",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Search Field
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    TextField(
                                        value = uiState.searchQuery,
                                        onValueChange = viewModel::onSearchQueryChanged,
                                        placeholder = {
                                            Text(
                                                "Cari judul atau isi teks dokumen (OCR)...",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        trailingIcon = {
                                            if (uiState.searchQuery.isNotEmpty()) {
                                                IconButton(
                                                    onClick = { viewModel.onSearchQueryChanged("") },
                                                    modifier = Modifier.testTag("clear_search_button")
                                                ) {
                                                    Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian")
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("search_text_field")
                                    )
                                }

                                // Category Filter Bar
                                CategoryChipBar(
                                    selectedCategory = uiState.selectedCategory,
                                    onCategorySelected = viewModel::onCategorySelected
                                )

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    thickness = 0.8.dp
                                )
                            }
                        }

                        // Grid / Empty state
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        ) {
                            if (uiState.isLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                                        modifier = Modifier.padding(32.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(28.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                            } else if (uiState.documents.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(90.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.DocumentScanner,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(44.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Text(
                                            text = if (uiState.searchQuery.isNotEmpty()) "Tidak ada hasil pencarian" else "Belum Ada Dokumen",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (uiState.searchQuery.isNotEmpty())
                                                "Coba kata kunci lain untuk judul atau isi teks dokumen."
                                            else
                                                "Pindai dokumen fisik dengan kamera atau pilih foto dari galeri Anda.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.fillMaxWidth(0.85f)
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Button(
                                            onClick = {
                                                scannerHelper?.startScan(
                                                    onIntentSenderReady = { request -> scannerLauncher.launch(request) },
                                                    onError = { }
                                                )
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Mulai Pindai Sekarang")
                                        }
                                    }
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 160.dp),
                                    contentPadding = PaddingValues(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(uiState.documents, key = { it.id }) { doc ->
                                        DocumentCard(
                                            document = doc,
                                            onClick = { previewDocument = doc },
                                            onShare = { sharePdf(doc) },
                                            onViewOcr = { ocrDocument = doc },
                                            onEdit = { editDocument = doc },
                                            onDelete = { viewModel.deleteDocument(doc) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                BottomNavItem.CONVERT -> {
                    // --- TAB 2: CONVERT ---
                    ConvertScreen(
                        repository = repository,
                        toolsManager = toolsManager,
                        documents = uiState.documents,
                        onDocumentCreated = { currentTab = BottomNavItem.DOCUMENTS }
                    )
                }

                BottomNavItem.COMPRESS -> {
                    // --- TAB 3: KOMPRES ---
                    CompressScreen(
                        repository = repository,
                        toolsManager = toolsManager,
                        documents = uiState.documents,
                        onDocumentCreated = { currentTab = BottomNavItem.DOCUMENTS }
                    )
                }

                BottomNavItem.READER -> {
                    // --- TAB 4: READER ---
                    ReaderScreen(
                        repository = repository,
                        toolsManager = toolsManager,
                        documents = uiState.documents
                    )
                }

                BottomNavItem.EDIT_PDF -> {
                    // --- TAB 5: EDIT PDF ---
                    EditPdfScreen(
                        repository = repository,
                        toolsManager = toolsManager,
                        documents = uiState.documents,
                        onDocumentCreated = { currentTab = BottomNavItem.DOCUMENTS }
                    )
                }

                BottomNavItem.HISTORY -> {
                    // --- TAB 6: HISTORY (RIWAYAT TINDAKAN) ---
                    HistoryScreen(
                        repository = repository
                    )
                }
            }
        }
    }

    // Modal: PDF Preview Dialog
    previewDocument?.let { doc ->
        PdfPreviewDialog(
            document = doc,
            onDismiss = { previewDocument = null },
            onShare = { sharePdf(doc) },
            onViewOcr = {
                val temp = doc
                previewDocument = null
                ocrDocument = temp
            }
        )
    }

    // Modal: OCR Text Dialog
    ocrDocument?.let { doc ->
        OcrTextDialog(
            document = doc,
            onDismiss = { ocrDocument = null }
        )
    }

    // Modal: Edit Document Dialog
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

    // Modal: Save Scanned Document Dialog
    uiState.pendingScanResult?.let { scanResult ->
        SaveScanDialog(
            pageCount = scanResult.pages?.size ?: 1,
            isFromGallery = false,
            onDismiss = viewModel::dismissPendingDialogs,
            onConfirm = { title, category ->
                viewModel.confirmSaveScan(title, category)
            }
        )
    }

    // Modal: Save Gallery Imported Document Dialog
    uiState.pendingGalleryUris?.let { uris ->
        SaveScanDialog(
            pageCount = uris.size,
            isFromGallery = true,
            onDismiss = viewModel::dismissPendingDialogs,
            onConfirm = { title, category ->
                viewModel.confirmSaveGallery(title, category)
            }
        )
    }
}
