package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertScreen(
    repository: DocumentRepository,
    toolsManager: PdfToolsManager,
    documents: List<DocumentEntity>,
    onDocumentCreated: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Gambar ke PDF", "Teks ke PDF", "PDF ke Gambar")

    var isProcessing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var progressFloat by remember { mutableFloatStateOf(0f) }

    // State for Text to PDF
    var textTitle by remember { mutableStateOf("Catatan Baru") }
    var textBody by remember { mutableStateOf("") }

    // State for Image to PDF
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var imageDocTitle by remember { mutableStateOf("Dokumen Foto") }

    // State for PDF to Images
    var selectedPdfDocument by remember { mutableStateOf<DocumentEntity?>(null) }
    var extractedImageFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    // Photo picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris = uris
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
                Text(
                    text = "Convert Dokumen",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Konversi gambar, teks, atau ekstrak halaman PDF",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Segmented Tab Row
        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        // Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // --- TAB 1: GAMBAR KE PDF ---
                        ConvertCard(
                            title = "Konversi Koleksi Foto ke File PDF",
                            description = "Pilih beberapa foto nota, lembar kerja, atau struk dari galeri untuk digabung menjadi 1 file PDF A4.",
                            icon = Icons.Default.Collections
                        ) {
                            OutlinedTextField(
                                value = imageDocTitle,
                                onValueChange = { imageDocTitle = it },
                                label = { Text("Nama Dokumen PDF") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedImageUris.isEmpty()) "Belum ada foto dipilih" else "${selectedImageUris.size} foto siap dikonversi",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selectedImageUris.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )

                                OutlinedButton(
                                    onClick = {
                                        imagePickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pilih Foto")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (selectedImageUris.isNotEmpty()) {
                                        isProcessing = true
                                        progressFloat = 0.15f
                                        statusText = "Mempersiapkan ${selectedImageUris.size} berkas foto..."
                                        coroutineScope.launch {
                                            try {
                                                progressFloat = 0.45f
                                                statusText = "Menyusun halaman PDF dan mengekstrak teks OCR..."
                                                repository.processAndSaveGalleryImages(
                                                    uris = selectedImageUris,
                                                    customTitle = imageDocTitle,
                                                    category = "Umum"
                                                )
                                                progressFloat = 1.0f
                                                statusText = "Selesai! PDF berhasil dibuat."
                                                selectedImageUris = emptyList()
                                                Toast.makeText(context, "PDF berhasil dibuat & disimpan!", Toast.LENGTH_SHORT).show()
                                                onDocumentCreated()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                            } finally {
                                                isProcessing = false
                                            }
                                        }
                                    }
                                },
                                enabled = selectedImageUris.isNotEmpty() && imageDocTitle.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("convert_images_to_pdf_btn")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Buat PDF Sekarang")
                            }
                        }
                    }

                    1 -> {
                        // --- TAB 2: TEKS KE PDF ---
                        ConvertCard(
                            title = "Konversi Teks / Catatan ke PDF",
                            description = "Tulis atau tempel teks dokumen, surat, atau catatan untuk diexport menjadi file PDF siap cetak dengan margin rapi.",
                            icon = Icons.Default.Description
                        ) {
                            OutlinedTextField(
                                value = textTitle,
                                onValueChange = { textTitle = it },
                                label = { Text("Judul Dokumen") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = textBody,
                                onValueChange = { textBody = it },
                                label = { Text("Isi Teks Dokumen") },
                                placeholder = { Text("Ketik atau tempel teks di sini...") },
                                minLines = 8,
                                maxLines = 15,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (textBody.isNotBlank()) {
                                        isProcessing = true
                                        progressFloat = 0.05f
                                        statusText = "Mempersiapkan tata letak teks..."
                                        coroutineScope.launch {
                                            try {
                                                val (pdfFile, thumbFile) = toolsManager.convertTextToPdf(
                                                    title = textTitle,
                                                    bodyText = textBody,
                                                    onProgress = { p, msg ->
                                                        progressFloat = p
                                                        statusText = msg
                                                    }
                                                )
                                                val newEntity = DocumentEntity(
                                                    title = textTitle,
                                                    category = "Catatan / Dokumen",
                                                    pageCount = 1,
                                                    thumbnailPath = thumbFile.absolutePath,
                                                    pdfPath = pdfFile.absolutePath,
                                                    ocrText = textBody
                                                )
                                                repository.insertCustomDocument(newEntity)
                                                Toast.makeText(context, "File PDF teks berhasil dibuat!", Toast.LENGTH_SHORT).show()
                                                textBody = ""
                                                onDocumentCreated()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                            } finally {
                                                isProcessing = false
                                            }
                                        }
                                    }
                                },
                                enabled = textBody.isNotBlank() && textTitle.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("convert_text_to_pdf_btn")
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Buat Dokumen PDF")
                            }
                        }
                    }

                    2 -> {
                        // --- TAB 3: PDF KE GAMBAR ---
                        ConvertCard(
                            title = "Ekstrak Halaman PDF ke File Gambar",
                            description = "Pilih dokumen PDF yang ada untuk mengekstrak setiap halaman menjadi file gambar JPEG beresolusi tinggi.",
                            icon = Icons.Default.BurstMode
                        ) {
                            if (documents.isEmpty()) {
                                Text(
                                    "Belum ada dokumen PDF di arsip Anda. Pindai atau buat dokumen terlebih dahulu.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                var dropdownExpanded by remember { mutableStateOf(false) }

                                ExposedDropdownMenuBox(
                                    expanded = dropdownExpanded,
                                    onExpandedChange = { dropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedPdfDocument?.title ?: "Pilih Dokumen PDF",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Pilih Dokumen PDF Sumber") },
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
                                                    selectedPdfDocument = doc
                                                    dropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        selectedPdfDocument?.let { doc ->
                                            isProcessing = true
                                            progressFloat = 0.05f
                                            statusText = "Membuka file PDF..."
                                            coroutineScope.launch {
                                                try {
                                                    val images = toolsManager.extractPagesAsImages(
                                                        sourcePdf = File(doc.pdfPath),
                                                        onProgress = { p, msg ->
                                                            progressFloat = p
                                                            statusText = msg
                                                        }
                                                    )
                                                    extractedImageFiles = images
                                                    Toast.makeText(context, "${images.size} gambar berhasil diekstrak!", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Gagal mengekstrak: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                } finally {
                                                    isProcessing = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = selectedPdfDocument != null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("extract_images_btn")
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ekstrak Semua Halaman")
                                }

                                if (extractedImageFiles.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                "${extractedImageFiles.size} File Gambar JPEG Tersimpan",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Gambar disimpan di cache lokal aplikasi: ${extractedImageFiles.firstOrNull()?.parentFile?.name}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isProcessing) {
        ProcessProgressDialog(
            title = when (selectedTabIndex) {
                0 -> "Sedang Membuat File PDF"
                1 -> "Sedang Menyusun Dokumen PDF"
                else -> "Sedang Mengekstrak Gambar"
            },
            statusMessage = statusText,
            progress = progressFloat
        )
    }
}

@Composable
fun ConvertCard(
    title: String,
    description: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}
