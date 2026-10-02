package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DocumentEntity
import com.example.data.repository.DocumentRepository
import com.example.tools.PdfToolsManager
import kotlinx.coroutines.launch

data class ToolItemData(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val circleColor: Color,
    val iconColor: Color,
    val description: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsTabScreen(
    repository: DocumentRepository,
    toolsManager: PdfToolsManager,
    documents: List<DocumentEntity>,
    onScanClick: () -> Unit,
    onImportImageClick: () -> Unit,
    onImportFileClick: () -> Unit,
    onLockAppClick: () -> Unit,
    onDocumentCreated: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var activeToolDetail by remember { mutableStateOf<ToolItemData?>(null) }
    var showWhatsNewDialog by remember { mutableStateOf(false) }

    val categories = listOf("Scan", "Impor", "Konversi", "Edit", "Utilitas")

    // --- 1. SCAN TOOLS ---
    val scanTools = listOf(
        ToolItemData("id_card", "Kartu ID", Icons.Default.Badge, Color(0xFFE6F7F2), Color(0xFF00A884), "Pindai KTP, SIM, atau Paspor otomatis dua sisi."),
        ToolItemData("extract_text", "Ekstrak Teks", Icons.Default.TextFields, Color(0xFFE6F7F2), Color(0xFF00A884), "Ekstrak teks dari foto dan dokumen dengan OCR presisi tinggi."),
        ToolItemData("passport_photo", "Pas Foto", Icons.Default.AccountBox, Color(0xFFEFF6FF), Color(0xFF3B82F6), "Hasilkan pas foto ukuran standar 2x3, 3x4, 4x6 dengan latar belakang rapi."),
        ToolItemData("formula", "Rumus", Icons.Default.Functions, Color(0xFFE8F5E9), Color(0xFF16A34A), "Pindai rumus matematika dan sains untuk konversi ke teks digital."),
        ToolItemData("photo_translate", "Penerjemahan foto", Icons.Default.Translate, Color(0xFFF3E8FF), Color(0xFF9333EA), "Terjemahkan teks dalam gambar ke bahasa Indonesia atau bahasa asing."),
        ToolItemData("book", "Buku", Icons.Default.MenuBook, Color(0xFFE0F2FE), Color(0xFF0284C7), "Mode pemindaian ganda otomatis meratakan lipatan lembaran buku."),
        ToolItemData("slide", "Slide", Icons.Default.Slideshow, Color(0xFFFFF7ED), Color(0xFFEA580C), "Pindai presentasi layar atau proyektor dengan penyesuaian sudut otomatis."),
        ToolItemData("whiteboard", "Papan Tulis", Icons.Default.CoPresent, Color(0xFFECFEFF), Color(0xFF06B6D4), "Hapus pantulan cahaya dan tingkatkan kontras tulisan papan tulis."),
        ToolItemData("timestamp", "Stempel waktu", Icons.Default.AccessTime, Color(0xFFEFF6FF), Color(0xFF2563EB), "Tambahkan watermark tanggal, waktu, dan lokasi pemotretan otomatis.")
    )

    // --- 2. IMPOR TOOLS ---
    val importTools = listOf(
        ToolItemData("import_image", "Impor Gambar", Icons.Default.Image, Color(0xFFE6F7F2), Color(0xFF00A884), "Pilih foto dari galeri HP dan satukan menjadi dokumen PDF."),
        ToolItemData("import_file", "Impor File", Icons.Default.DriveFileMove, Color(0xFFEEF2FF), Color(0xFF4F46E5), "Impor berkas dokumen dari penyimpanan internal atau unduhan.")
    )

    // --- 3. KONVERSI TOOLS ---
    val conversionTools = listOf(
        ToolItemData("to_word", "Ke Word", Icons.Default.Description, Color(0xFFEFF6FF), Color(0xFF2563EB), "Konversi teks dokumen hasil scan ke format Microsoft Word."),
        ToolItemData("to_excel", "Ke Excel", Icons.Default.TableChart, Color(0xFFE8F5E9), Color(0xFF16A34A), "Ekstrak tabel dan angka dari struk/laporan langsung ke spreadsheet Excel."),
        ToolItemData("to_ppt", "Ke PPT", Icons.Default.Slideshow, Color(0xFFFFF1F2), Color(0xFFE11D48), "Konversi lembar dokumen menjadi presentasi slide PowerPoint."),
        ToolItemData("pdf_to_image", "PDF ke Gambar", Icons.Default.Collections, Color(0xFFE6F7F2), Color(0xFF00A884), "Ekstrak setiap halaman berkas PDF menjadi foto JPEG berkualitas tinggi."),
        ToolItemData("pdf_to_long_image", "PDF ke Gambar Panjang", Icons.Default.Panorama, Color(0xFFECFEFF), Color(0xFF0891B2), "Gabungkan seluruh halaman PDF menjadi satu gambar gulung panjang vertikal.")
    )

    // --- 4. EDIT TOOLS ---
    val editTools = listOf(
        ToolItemData("signature", "Tanda tangani", Icons.Default.Draw, Color(0xFFE6F7F2), Color(0xFF00A884), "Bubuhkan tanda tangan digital pada dokumen secara instan."),
        ToolItemData("add_watermark", "Tambah Tanda Air", Icons.Default.BrandingWatermark, Color(0xFFEFF6FF), Color(0xFF2563EB), "Beri cap teks atau tanda air untuk melindungi hak cipta dokumen."),
        ToolItemData("smart_erase", "Hapus Cerdas", Icons.Default.AutoFixHigh, Color(0xFFE6F7F2), Color(0xFF00A884), "Hapus coretan, bayangan, atau jari tangan pada tepi dokumen secara cerdas."),
        ToolItemData("remove_marker", "Hapus Penanda", Icons.Default.HighlightOff, Color(0xFFEEF2FF), Color(0xFF4F46E5), "Bersihkan stabilo atau coretan pena dari teks buku pelajaran."),
        ToolItemData("restore_photo", "Pulihkan Foto", Icons.Default.AutoAwesome, Color(0xFFFFF7ED), Color(0xFFEA580C), "Tingkatkan ketajaman dan perbaiki warna foto lawas atau buram."),
        ToolItemData("merge_files", "Gabungkan File", Icons.Default.CallMerge, Color(0xFFECFEFF), Color(0xFF0891B2), "Satukan beberapa file PDF terpisah menjadi satu dokumen utuh."),
        ToolItemData("split_pdf", "Pisah Halaman PDF", Icons.Default.CallSplit, Color(0xFFEFF6FF), Color(0xFF2563EB), "Bagi atau pisahkan halaman-halaman PDF menjadi file tersendiri."),
        ToolItemData("reorder_pages", "Urutkan Ulang Halaman", Icons.Default.SwapVert, Color(0xFFEEF2FF), Color(0xFF4F46E5), "Atur kembali susunan urutan halaman sesuai keinginan Anda."),
        ToolItemData("lock_pdf", "Kunci", Icons.Default.Lock, Color(0xFFE8F5E9), Color(0xFF16A34A), "Kunci dan sembunyikan dokumen penting di ruang privat yang terlindungi."),
        ToolItemData("compress_pdf", "Kompres", Icons.Default.FolderZip, Color(0xFFEFF6FF), Color(0xFF2563EB), "Perkecil ukuran dokumen PDF tanpa mengurangi keterbacaan teks.")
    )

    // --- 5. UTILITAS TOOLS ---
    val utilityTools = listOf(
        ToolItemData("solver_ai", "Solver AI", Icons.Default.Psychology, Color(0xFFE6F7F2), Color(0xFF00A884), "Bantu cari jawaban dan penjelasan tugas dengan pemindaian cerdas."),
        ToolItemData("count_cam", "CountCam", Icons.Default.Tag, Color(0xFFECFEFF), Color(0xFF0891B2), "Hitung jumlah benda (baut, pipa, buku, obat) otomatis hanya dari foto."),
        ToolItemData("print_doc", "Cetak", Icons.Default.Print, Color(0xFFE6F7F2), Color(0xFF00A884), "Cetak dokumen PDF langsung ke printer nirkabel (Wireless Printer)."),
        ToolItemData("qr_scanner", "Scan Kode QR", Icons.Default.QrCodeScanner, Color(0xFFE6F7F2), Color(0xFF00A884), "Pindai tautan, Wi-Fi, dan teks dari berbagai jenis kode QR dan barcode.")
    )

    val onToolItemClicked = { tool: ToolItemData ->
        when (tool.id) {
            "id_card", "formula", "book", "slide", "whiteboard", "timestamp", "passport_photo", "photo_translate" -> {
                Toast.makeText(context, "Mode ${tool.title} diaktifkan. Membuka kamera...", Toast.LENGTH_SHORT).show()
                onScanClick()
            }
            "import_image" -> onImportImageClick()
            "import_file" -> onImportFileClick()
            "qr_scanner" -> {
                Toast.makeText(context, "Membuka scanner kode QR...", Toast.LENGTH_SHORT).show()
                onScanClick()
            }
            "lock_pdf" -> {
                Toast.makeText(context, "Mengunci aplikasi...", Toast.LENGTH_SHORT).show()
                onLockAppClick()
            }
            else -> {
                // Show rich feature preview & execution modal
                activeToolDetail = tool
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // --- TOP BAR: "Alat" + "Yang Baru" badge + Search Icon ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Alat",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // [ 📄 Yang Baru 🔴 ] Pill Button
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFE6F7F2),
                    modifier = Modifier
                        .clickable { showWhatsNewDialog = true }
                        .padding(end = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF00A884),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Yang Baru",
                            color = Color(0xFF00A884),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        // Red notification dot
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.size(6.dp)
                        ) {}
                    }
                }

                // Search Icon
                IconButton(
                    onClick = { isSearchActive = !isSearchActive },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Cari Fitur Alat",
                        tint = Color(0xFF334155),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Search Input (Collapsible)
        if (isSearchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari nama alat (mis. Kompres, Word, Pas Foto)...") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00A884),
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedContainerColor = Color(0xFFF8FAFC)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        // --- CATEGORY TABS (Scan, Impor, Konversi, Edit, Utilitas) ---
        val scrollTabState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollTabState)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            categories.forEachIndexed { index, cat ->
                val isSelected = selectedCategoryIndex == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            selectedCategoryIndex = index
                            coroutineScope.launch {
                                // Scroll lazy column to target section item index
                                val targetIndex = when (index) {
                                    0 -> 0 // Scan
                                    1 -> 1 // Impor
                                    2 -> 2 // Konversi
                                    3 -> 3 // Edit
                                    else -> 4 // Utilitas
                                }
                                listState.animateScrollToItem(targetIndex)
                            }
                        }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = cat,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF00A884) else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = Color(0xFF00A884),
                            modifier = Modifier
                                .width(28.dp)
                                .height(3.dp)
                        ) {}
                    } else {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

        // --- CONTENT SECTIONS ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp)
        ) {
            // Filter helper
            val filterTools: (List<ToolItemData>) -> List<ToolItemData> = { list ->
                if (searchQuery.isBlank()) list else list.filter { it.title.contains(searchQuery, ignoreCase = true) }
            }

            // 1. Scan Section
            val filteredScan = filterTools(scanTools)
            if (filteredScan.isNotEmpty()) {
                item {
                    ToolCategorySection(
                        title = "Scan",
                        tools = filteredScan,
                        onItemClick = onToolItemClicked
                    )
                }
            }

            // 2. Impor Section
            val filteredImport = filterTools(importTools)
            if (filteredImport.isNotEmpty()) {
                item {
                    ToolCategorySection(
                        title = "Impor",
                        tools = filteredImport,
                        onItemClick = onToolItemClicked
                    )
                }
            }

            // 3. Konversi Section
            val filteredConversion = filterTools(conversionTools)
            if (filteredConversion.isNotEmpty()) {
                item {
                    ToolCategorySection(
                        title = "Konversi",
                        tools = filteredConversion,
                        onItemClick = onToolItemClicked
                    )
                }
            }

            // 4. Edit Section
            val filteredEdit = filterTools(editTools)
            if (filteredEdit.isNotEmpty()) {
                item {
                    ToolCategorySection(
                        title = "Edit",
                        tools = filteredEdit,
                        onItemClick = onToolItemClicked
                    )
                }
            }

            // 5. Utilitas Section
            val filteredUtility = filterTools(utilityTools)
            if (filteredUtility.isNotEmpty()) {
                item {
                    ToolCategorySection(
                        title = "Utilitas",
                        tools = filteredUtility,
                        onItemClick = onToolItemClicked
                    )
                }
            }
        }
    }

    // Detail Action Dialog / Modal
    activeToolDetail?.let { tool ->
        AlertDialog(
            onDismissRequest = { activeToolDetail = null },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = tool.circleColor,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(tool.icon, contentDescription = null, tint = tool.iconColor, modifier = Modifier.size(30.dp))
                    }
                }
            },
            title = {
                Text(
                    text = tool.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tool.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF475569),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (documents.isNotEmpty()) {
                        Text(
                            text = "Pilih dokumen sumber dari daftar dokumen Anda, atau lakukan pemindaian baru.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = "Belum ada dokumen tersimpan. Silakan pindai atau impor berkas terlebih dahulu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        activeToolDetail = null
                        onScanClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884))
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pindai Dokumen Baru")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeToolDetail = null }) {
                    Text("Tutup", color = Color(0xFF64748B))
                }
            }
        )
    }

    // "Yang Baru" Informative Dialog
    if (showWhatsNewDialog) {
        AlertDialog(
            onDismissRequest = { showWhatsNewDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Celebration, contentDescription = null, tint = Color(0xFF00A884))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Yang Baru di Versi 1.0")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("✨ Desain baru CamScanner Pro yang bersih dan modern.", fontSize = 14.sp)
                    Text("⚡ Dukungan 30+ Fitur Alat lengkap (Scan, Konversi, Edit, Utilitas).", fontSize = 14.sp)
                    Text("🔒 100% Offline tanpa internet, data dokumen Anda aman dan privat.", fontSize = 14.sp)
                    Text("📦 Kompatibel dengan format PDF, Word, Excel, dan Gambar resolusi tinggi.", fontSize = 14.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showWhatsNewDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884))
                ) {
                    Text("Mengerti")
                }
            }
        )
    }
}

@Composable
fun ToolCategorySection(
    title: String,
    tools: List<ToolItemData>,
    onItemClick: (ToolItemData) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        // Section Title
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        // 4 Columns Flow Row
        val chunkedRows = tools.chunked(4)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            chunkedRows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    rowItems.forEach { tool ->
                        ToolGridItem(tool = tool, onClick = { onItemClick(tool) })
                    }
                    // Spacer for incomplete row
                    if (rowItems.size < 4) {
                        repeat(4 - rowItems.size) {
                            Spacer(modifier = Modifier.width(76.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolGridItem(
    tool: ToolItemData,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clickable { onClick() }
            .padding(vertical = 4.dp)
            .testTag("tool_item_${tool.id}")
    ) {
        Surface(
            shape = CircleShape,
            color = tool.circleColor,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = tool.iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = tool.title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF334155),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp
        )
    }
}
