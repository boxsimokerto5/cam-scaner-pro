package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.DocumentEntity
import com.example.data.repository.DocumentRepository
import com.example.tools.PdfToolsManager
import com.example.ui.components.ProcessProgressDialog
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

enum class CompressionLevel(
    val title: String,
    val description: String,
    val scaleFactor: Float,
    val jpegQuality: Int,
    val estSaving: String
) {
    MEDIUM("Sedang (Rekomendasi)", "Keseimbangan ideal antara ukuran kecil dan ketajaman teks", 0.75f, 70, "Hemat ~45%"),
    HIGH("Kuat (Ukuran Terkecil)", "Cocok untuk upload web/email dengan batas ukuran ketat", 0.50f, 50, "Hemat ~70%"),
    LOW("Ringan (Kualitas Tinggi)", "Menjaga resolusi tinggi, hanya mereduksi metadata", 0.90f, 85, "Hemat ~25%")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressScreen(
    repository: DocumentRepository,
    toolsManager: PdfToolsManager,
    documents: List<DocumentEntity>,
    onDocumentCreated: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedDoc by remember { mutableStateOf<DocumentEntity?>(null) }
    var selectedLevel by remember { mutableStateOf(CompressionLevel.MEDIUM) }

    var isCompressing by remember { mutableStateOf(false) }
    var compressProgress by remember { mutableFloatStateOf(0f) }
    var compressMessage by remember { mutableStateOf("Mempersiapkan...") }
    var compressedResultFile by remember { mutableStateOf<File?>(null) }
    var originalSizeBytes by remember { mutableLongStateOf(0L) }
    var compressedSizeBytes by remember { mutableLongStateOf(0L) }

    // External file picker
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val temp = toolsManager.copyUriToTempFile(uri, "picked_external.pdf")
                    selectedDoc = DocumentEntity(
                        id = -1,
                        title = "PDF Eksternal",
                        category = "Eksternal",
                        pageCount = 1,
                        thumbnailPath = "",
                        pdfPath = temp.absolutePath
                    )
                    originalSizeBytes = temp.length()
                    compressedResultFile = null
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal memuat PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(selectedDoc) {
        selectedDoc?.let {
            val file = File(it.pdfPath)
            if (file.exists()) {
                originalSizeBytes = file.length()
            }
            compressedResultFile = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(
                    text = "Kompres PDF",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Kecilkan ukuran file PDF tanpa mengurangi keterbacaan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Document Selection Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "1. Pilih Dokumen PDF",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (documents.isNotEmpty()) {
                        var dropdownExpanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedDoc?.title ?: "Pilih dari arsip Anda...",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                documents.forEach { doc ->
                                    DropdownMenuItem(
                                        text = { Text("${doc.title} (${formatFileSize(File(doc.pdfPath).length())})") },
                                        onClick = {
                                            selectedDoc = doc
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedButton(
                        onClick = { pdfPickerLauncher.launch("application/pdf") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pilih File PDF dari Memori HP")
                    }

                    if (selectedDoc != null && originalSizeBytes > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = selectedDoc?.title ?: "",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Ukuran Asli: ${formatFileSize(originalSizeBytes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Compression Level Options
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "2. Tingkat Kompresi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    CompressionLevel.values().forEach { level ->
                        val isSelected = selectedLevel == level
                        Surface(
                            onClick = { selectedLevel = level },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedLevel = level }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = level.title,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = level.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = level.estSaving,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Compress Action Button
            Button(
                onClick = {
                    val doc = selectedDoc ?: return@Button
                    compressProgress = 0.05f
                    compressMessage = "Mempersiapkan dokumen..."
                    isCompressing = true
                    coroutineScope.launch {
                        try {
                            val sourceFile = File(doc.pdfPath)
                            val compressed = toolsManager.compressPdf(
                                sourcePdf = sourceFile,
                                scaleFactor = selectedLevel.scaleFactor,
                                jpegQuality = selectedLevel.jpegQuality,
                                onProgress = { p, msg ->
                                    compressProgress = p
                                    compressMessage = msg
                                }
                            )
                            compressedResultFile = compressed
                            compressedSizeBytes = compressed.length()
                            Toast.makeText(context, "Kompresi selesai!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        } finally {
                            isCompressing = false
                        }
                    }
                },
                enabled = selectedDoc != null && !isCompressing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("start_compress_btn")
            ) {
                Icon(Icons.Default.Compress, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mulai Kompres Dokumen", fontWeight = FontWeight.Bold)
            }

            // Result Card
            if (compressedResultFile != null && compressedSizeBytes > 0) {
                val percentSaved = if (originalSizeBytes > 0) {
                    ((originalSizeBytes - compressedSizeBytes).toFloat() / originalSizeBytes * 100).toInt().coerceAtLeast(0)
                } else 0

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Berhasil Dikompres!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "Hemat ruang sebesar $percentSaved%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Ukuran Awal", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    formatFileSize(originalSizeBytes),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.align(Alignment.CenterVertically))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Ukuran Baru", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    formatFileSize(compressedSizeBytes),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    compressedResultFile?.let { file ->
                                        val shareUri = repository.getShareablePdfUri(file.absolutePath)
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, shareUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Bagikan PDF"))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bagikan")
                            }

                            Button(
                                onClick = {
                                    compressedResultFile?.let { file ->
                                        coroutineScope.launch {
                                            val newEntity = DocumentEntity(
                                                title = "${selectedDoc?.title ?: "Dokumen"} (Terkecil)",
                                                category = selectedDoc?.category ?: "Umum",
                                                pageCount = selectedDoc?.pageCount ?: 1,
                                                thumbnailPath = selectedDoc?.thumbnailPath ?: "",
                                                pdfPath = file.absolutePath,
                                                ocrText = selectedDoc?.ocrText ?: ""
                                            )
                                            repository.insertCustomDocument(newEntity)
                                            Toast.makeText(context, "Tersimpan ke Dokumen Saya!", Toast.LENGTH_SHORT).show()
                                            onDocumentCreated()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simpan")
                            }
                        }
                    }
                }
            }
        }
    }

    if (isCompressing) {
        ProcessProgressDialog(
            title = "Sedang Mengompres PDF",
            statusMessage = compressMessage,
            progress = compressProgress
        )
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.getDefault(), "%.1f MB", mb)
    } else {
        String.format(Locale.getDefault(), "%.0f KB", kb)
    }
}
