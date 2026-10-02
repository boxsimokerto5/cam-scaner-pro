package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.DocumentEntity
import com.example.data.repository.DocumentRepository
import com.example.tools.PdfToolsManager
import com.example.ui.components.OcrTextDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ReaderTheme(val title: String, val bg: Color, val cardBg: Color, val textColor: Color) {
    LIGHT("Terang", Color(0xFFF8FAFC), Color(0xFFFFFFFF), Color(0xFF0F172A)),
    SEPIA("Sepia", Color(0xFFF4ECD8), Color(0xFFFDFBF7), Color(0xFF4A3E31)),
    DARK("Gelap", Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFFF8FAFC))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    repository: DocumentRepository,
    toolsManager: PdfToolsManager,
    documents: List<DocumentEntity>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var selectedDocument by remember { mutableStateOf<DocumentEntity?>(documents.firstOrNull()) }
    var readerTheme by remember { mutableStateOf(ReaderTheme.LIGHT) }
    var renderedPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoadingPages by remember { mutableStateOf(false) }
    var showOcrDialog by remember { mutableStateOf(false) }

    // External PDF picker
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val temp = toolsManager.copyUriToTempFile(uri, "opened_reader.pdf")
                    selectedDocument = DocumentEntity(
                        id = -1,
                        title = "Dokumen Eksternal",
                        category = "Eksternal",
                        pageCount = 1,
                        thumbnailPath = "",
                        pdfPath = temp.absolutePath,
                        ocrText = ""
                    )
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal membuka PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Load PDF bitmaps whenever selected document changes
    LaunchedEffect(selectedDocument?.pdfPath) {
        val path = selectedDocument?.pdfPath ?: return@LaunchedEffect
        val file = File(path)
        if (!file.exists()) {
            renderedPages = emptyList()
            return@LaunchedEffect
        }

        isLoadingPages = true
        withContext(Dispatchers.IO) {
            try {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                val bitmaps = mutableListOf<Bitmap>()

                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val scale = 2
                    val bmp = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmaps.add(bmp)
                }
                renderer.close()
                pfd.close()

                withContext(Dispatchers.Main) {
                    renderedPages = bitmaps
                    isLoadingPages = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoadingPages = false
                    Toast.makeText(context, "Error render PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(readerTheme.bg)
            .statusBarsPadding()
    ) {
        // Reader Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedDocument?.title ?: "PDF Reader",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = if (renderedPages.isNotEmpty()) "${renderedPages.size} Halaman" else "Pilih dokumen untuk membaca",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Open external PDF button
                        IconButton(onClick = { pdfPickerLauncher.launch("application/pdf") }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Buka PDF")
                        }

                        // Share
                        selectedDocument?.let { doc ->
                            IconButton(
                                onClick = {
                                    val shareUri = repository.getShareablePdfUri(doc.pdfPath)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, shareUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Bagikan"))
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Bagikan")
                            }
                        }

                        // OCR text
                        if (selectedDocument != null && selectedDocument?.ocrText?.isNotBlank() == true) {
                            IconButton(onClick = { showOcrDialog = true }) {
                                Icon(Icons.Default.TextFields, contentDescription = "Lihat OCR")
                            }
                        }
                    }
                }

                // Document Picker Selector & Theme Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Dropdown selector
                    if (documents.isNotEmpty()) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Ganti Dokumen", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                documents.forEach { doc ->
                                    DropdownMenuItem(
                                        text = { Text(doc.title) },
                                        onClick = {
                                            selectedDocument = doc
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Theme selector buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReaderTheme.values().forEach { theme ->
                            val isSelected = readerTheme == theme
                            Surface(
                                onClick = { readerTheme = theme },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text(
                                    text = theme.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

        // Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                isLoadingPages -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Memuat lembaran dokumen...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = readerTheme.textColor
                        )
                    }
                }

                selectedDocument == null || renderedPages.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Pilih dokumen PDF untuk dibaca",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = readerTheme.textColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Buka file PDF dari arsip atau memori HP untuk kenyamanan membaca optimal.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = readerTheme.textColor.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { pdfPickerLauncher.launch("application/pdf") }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buka dari File HP")
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(renderedPages) { index, bitmap ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = readerTheme.cardBg),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(readerTheme.cardBg.copy(alpha = 0.8f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Halaman ${index + 1} dari ${renderedPages.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = readerTheme.textColor.copy(alpha = 0.6f)
                                        )
                                    }
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Halaman ${index + 1}",
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOcrDialog && selectedDocument != null) {
        OcrTextDialog(
            document = selectedDocument!!,
            onDismiss = { showOcrDialog = false }
        )
    }
}
