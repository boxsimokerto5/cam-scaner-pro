package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.DocumentEntity
import com.example.data.repository.DocumentRepository
import com.example.tools.PdfToolsManager
import com.example.ui.components.ProcessProgressDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPdfScreen(
    repository: DocumentRepository,
    toolsManager: PdfToolsManager,
    documents: List<DocumentEntity>,
    onDocumentCreated: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedDocument by remember { mutableStateOf<DocumentEntity?>(documents.firstOrNull()) }
    var pageBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var activePageRotations by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) } // page index -> degrees
    var deletedPageIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isLoadingPages by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var editProgress by remember { mutableFloatStateOf(0f) }
    var editStatusMessage by remember { mutableStateOf("Memproses dokumen...") }

    // Watermark dialog state
    var showWatermarkDialog by remember { mutableStateOf(false) }
    var watermarkText by remember { mutableStateOf("") }

    // Merge dialog state
    var showMergeDialog by remember { mutableStateOf(false) }

    // Load PDF pages into bitmaps
    LaunchedEffect(selectedDocument?.pdfPath) {
        val path = selectedDocument?.pdfPath ?: return@LaunchedEffect
        val file = File(path)
        if (!file.exists()) {
            pageBitmaps = emptyList()
            return@LaunchedEffect
        }

        isLoadingPages = true
        activePageRotations = emptyMap()
        deletedPageIndices = emptySet()

        withContext(Dispatchers.IO) {
            try {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                val bitmaps = mutableListOf<Bitmap>()

                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmaps.add(bmp)
                }
                renderer.close()
                pfd.close()

                withContext(Dispatchers.Main) {
                    pageBitmaps = bitmaps
                    isLoadingPages = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoadingPages = false
                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Edit & Tata Halaman PDF",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Putar halaman, hapus lembar, tambah watermark, atau gabung PDF",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Document selector dropdown
                if (documents.isNotEmpty()) {
                    var dropdownExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedDocument?.title ?: "Pilih Dokumen",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pilih Dokumen untuk Diedit") },
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
                                    text = { Text("${doc.title} (${doc.pageCount} Hal)") },
                                    onClick = {
                                        selectedDocument = doc
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Quick Action Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Watermark button
                    OutlinedButton(
                        onClick = { showWatermarkDialog = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.BrandingWatermark, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Watermark", style = MaterialTheme.typography.labelSmall)
                    }

                    // Merge PDF button
                    OutlinedButton(
                        onClick = { showMergeDialog = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.MergeType, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gabung PDF", style = MaterialTheme.typography.labelSmall)
                    }

                    // Reset changes
                    OutlinedButton(
                        onClick = {
                            activePageRotations = emptyMap()
                            deletedPageIndices = emptySet()
                            watermarkText = ""
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        // Content: Grid of pages
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
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Memuat tata letak halaman...")
                    }
                }

                selectedDocument == null || pageBitmaps.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Pilih dokumen terlebih dahulu untuk mulai mengedit.")
                    }
                }

                else -> {
                    val keptPagesCount = pageBitmaps.size - deletedPageIndices.size

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Subtitle status
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Menampilkan $keptPagesCount dari ${pageBitmaps.size} halaman",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (watermarkText.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Watermark: $watermarkText",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 140.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            itemsIndexed(pageBitmaps) { index, bitmap ->
                                val isDeleted = deletedPageIndices.contains(index)
                                val rotation = activePageRotations[index] ?: 0

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDeleted) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDeleted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(150.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Halaman ${index + 1}",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .rotate(rotation.toFloat())
                                            )

                                            // Page badge
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color.Black.copy(alpha = 0.7f),
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(6.dp)
                                            ) {
                                                Text(
                                                    text = "Hal ${index + 1}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            if (isDeleted) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Actions: Rotate & Delete/Restore
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    val current = activePageRotations[index] ?: 0
                                                    activePageRotations = activePageRotations + (index to (current + 90) % 360)
                                                },
                                                enabled = !isDeleted,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.RotateRight, contentDescription = "Putar", modifier = Modifier.size(18.dp))
                                            }

                                            IconButton(
                                                onClick = {
                                                    deletedPageIndices = if (isDeleted) {
                                                        deletedPageIndices - index
                                                    } else {
                                                        deletedPageIndices + index
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    if (isDeleted) Icons.Default.Restore else Icons.Default.Delete,
                                                    contentDescription = if (isDeleted) "Kembalikan" else "Hapus",
                                                    tint = if (isDeleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Save Button Bar
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                Button(
                                    onClick = {
                                        val doc = selectedDocument ?: return@Button
                                        val keptPages = (0 until pageBitmaps.size).filter { !deletedPageIndices.contains(it) }
                                        if (keptPages.isEmpty()) {
                                            Toast.makeText(context, "Tidak boleh menghapus semua halaman!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        isSaving = true
                                        editProgress = 0.05f
                                        editStatusMessage = "Mempersiapkan struktur dokumen..."
                                        coroutineScope.launch {
                                            try {
                                                val (outFile, thumbFile) = toolsManager.editPdf(
                                                    sourcePdf = File(doc.pdfPath),
                                                    pagesToKeep = keptPages,
                                                    rotations = activePageRotations,
                                                    watermarkText = watermarkText.ifBlank { null },
                                                    onProgress = { p, msg ->
                                                        editProgress = p
                                                        editStatusMessage = msg
                                                    }
                                                )

                                                val newEntity = DocumentEntity(
                                                    title = "${doc.title} (Edited)",
                                                    category = doc.category,
                                                    pageCount = keptPages.size,
                                                    thumbnailPath = thumbFile.absolutePath,
                                                    pdfPath = outFile.absolutePath,
                                                    ocrText = doc.ocrText
                                                )
                                                repository.insertCustomDocument(newEntity)
                                                Toast.makeText(context, "PDF Hasil Edit berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                                onDocumentCreated()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Gagal mengedit PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                            } finally {
                                                isSaving = false
                                            }
                                        }
                                    },
                                    enabled = !isSaving && keptPagesCount > 0,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("save_edited_pdf_btn")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Simpan Versi Baru (${keptPagesCount} Hal)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Watermark Dialog
    if (showWatermarkDialog) {
        var tempWatermark by remember { mutableStateOf(watermarkText) }
        val presets = listOf("RAHASIA", "CONFIDENTIAL", "LUNAS", "DRAFT", "ORIGINAL")

        AlertDialog(
            onDismissRequest = { showWatermarkDialog = false },
            title = { Text("Tambahkan Watermark") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Watermark teks akan dicetak secara diagonal semi-transparan di setiap halaman dokumen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = tempWatermark,
                        onValueChange = { tempWatermark = it },
                        label = { Text("Teks Watermark") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Pilihan Cepat:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.take(3).forEach { preset ->
                            SuggestionChip(
                                onClick = { tempWatermark = preset },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        watermarkText = tempWatermark
                        showWatermarkDialog = false
                    }
                ) {
                    Text("Terapkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWatermarkDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Merge PDF Dialog
    if (showMergeDialog) {
        val otherDocs = documents.filter { it.id != selectedDocument?.id }
        var docToMerge by remember { mutableStateOf<DocumentEntity?>(otherDocs.firstOrNull()) }

        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            title = { Text("Gabung Dokumen PDF") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Pilih dokumen lain dari arsip Anda untuk digabungkan di bagian akhir dokumen ini.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (otherDocs.isEmpty()) {
                        Text(
                            "Tidak ada dokumen lain yang tersedia untuk digabung.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = docToMerge?.title ?: "Pilih dokumen...",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                otherDocs.forEach { doc ->
                                    DropdownMenuItem(
                                        text = { Text("${doc.title} (${doc.pageCount} Hal)") },
                                        onClick = {
                                            docToMerge = doc
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val baseDoc = selectedDocument ?: return@Button
                        val appendDoc = docToMerge ?: return@Button
                        showMergeDialog = false
                        isSaving = true
                        editProgress = 0.05f
                        editStatusMessage = "Mempersiapkan penggabungan dokumen..."

                        coroutineScope.launch {
                            try {
                                val (mergedFile, thumbFile) = toolsManager.mergePdfs(
                                    pdfFiles = listOf(File(baseDoc.pdfPath), File(appendDoc.pdfPath)),
                                    mergedTitle = "${baseDoc.title}_merged",
                                    onProgress = { p, msg ->
                                        editProgress = p
                                        editStatusMessage = msg
                                    }
                                )
                                val newEntity = DocumentEntity(
                                    title = "${baseDoc.title} + ${appendDoc.title}",
                                    category = baseDoc.category,
                                    pageCount = baseDoc.pageCount + appendDoc.pageCount,
                                    thumbnailPath = thumbFile.absolutePath,
                                    pdfPath = mergedFile.absolutePath,
                                    ocrText = "${baseDoc.ocrText}\n\n${appendDoc.ocrText}"
                                )
                                repository.insertCustomDocument(newEntity)
                                Toast.makeText(context, "Dokumen berhasil digabungkan!", Toast.LENGTH_SHORT).show()
                                onDocumentCreated()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal menggabungkan: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = otherDocs.isNotEmpty() && docToMerge != null
                ) {
                    Text("Gabungkan Sekarang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMergeDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (isSaving) {
        ProcessProgressDialog(
            title = "Sedang Memproses Dokumen PDF",
            statusMessage = editStatusMessage,
            progress = editProgress
        )
    }
}
