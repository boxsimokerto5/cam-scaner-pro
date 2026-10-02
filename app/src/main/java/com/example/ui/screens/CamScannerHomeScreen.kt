package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DocumentEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CamScannerHomeScreen(
    documents: List<DocumentEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onScanClick: () -> Unit,
    onPdfToolsClick: () -> Unit,
    onImportImageClick: () -> Unit,
    onImportFileClick: () -> Unit,
    onIdCardClick: () -> Unit,
    onSignClick: () -> Unit,
    onQrScanClick: () -> Unit,
    onAllToolsClick: () -> Unit,
    onViewAllDocumentsClick: () -> Unit,
    onDocumentClick: (DocumentEntity) -> Unit,
    onShareClick: (DocumentEntity) -> Unit,
    onOcrClick: (DocumentEntity) -> Unit
) {
    var checkedMap by remember { mutableStateOf<Map<Long, Boolean>>(emptyMap()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // --- 1. TOP BAR: SEARCH & VIP BADGES ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search Pill Field
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFF3F4F6),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Cari",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 15.sp
                                )
                            }
                            TextField(
                                value = searchQuery,
                                onValueChange = onSearchChange,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_search_input")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // [ ♾️ Free ] Pill Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(0.8.dp, Color(0xFFFCD34D)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AllInclusive,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Free",
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Crown VIP Icon
                IconButton(
                    onClick = onAllToolsClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "VIP",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // --- 2. QUICK ACTIONS (2 ROWS x 4 COLS) ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    QuickActionButton(
                        icon = Icons.Default.DocumentScanner,
                        label = "Scan",
                        circleColor = Color(0xFFE6F7F2),
                        iconColor = Color(0xFF00A884),
                        onClick = onScanClick
                    )
                    QuickActionButton(
                        icon = Icons.Default.PictureAsPdf,
                        label = "Alat PDF",
                        circleColor = Color(0xFFFEE2E2),
                        iconColor = Color(0xFFEF4444),
                        onClick = onPdfToolsClick
                    )
                    QuickActionButton(
                        icon = Icons.Default.Image,
                        label = "Impor Gambar",
                        circleColor = Color(0xFFEFF6FF),
                        iconColor = Color(0xFF3B82F6),
                        onClick = onImportImageClick
                    )
                    QuickActionButton(
                        icon = Icons.Default.DriveFileMove,
                        label = "Impor File",
                        circleColor = Color(0xFFEEF2FF),
                        iconColor = Color(0xFF6366F1),
                        onClick = onImportFileClick
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    QuickActionButton(
                        icon = Icons.Default.Badge,
                        label = "Kartu ID",
                        circleColor = Color(0xFFECFEFF),
                        iconColor = Color(0xFF06B6D4),
                        onClick = onIdCardClick
                    )
                    QuickActionButton(
                        icon = Icons.Default.Draw,
                        label = "Tanda tangani",
                        circleColor = Color(0xFFE6F7F2),
                        iconColor = Color(0xFF00A884),
                        hasFreeBadge = true,
                        onClick = onSignClick
                    )
                    QuickActionButton(
                        icon = Icons.Default.QrCodeScanner,
                        label = "Scan Kode QR",
                        circleColor = Color(0xFFE6F7F2),
                        iconColor = Color(0xFF0D9488),
                        onClick = onQrScanClick
                    )
                    QuickActionButton(
                        icon = Icons.Default.GridView,
                        label = "Semua",
                        circleColor = Color(0xFFE0F2FE),
                        iconColor = Color(0xFF0284C7),
                        onClick = onAllToolsClick
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        // --- 3. SECTION "TERKINI" HEADER ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Terkini",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B),
                    fontSize = 18.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onViewAllDocumentsClick() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Lihat Semua",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // --- 4. LIST DOKUMEN TERKINI ---
        if (documents.isEmpty()) {
            // Placeholder Demo matching the exact visual in screenshot
            item {
                FeaturedDocumentCard(
                    title = "CamScanner 28-09-2026 11.57",
                    dateSubtitle = "02/10/2026 17:24  |  📄 1",
                    thumbnailPath = null,
                    isChecked = false,
                    onCheckedChange = {},
                    onShareClick = onScanClick,
                    onWordClick = onScanClick,
                    onViewClick = onScanClick
                )
            }
            item {
                StandardDocumentRow(
                    title = "Jadwal_Dinas_...ptember_2026",
                    dateSubtitle = "01/10/2026 09:39",
                    isExcel = true,
                    thumbnailPath = null,
                    isChecked = false,
                    onCheckedChange = {},
                    onClick = onScanClick
                )
            }
            item {
                StandardDocumentRow(
                    title = "CamScanner 01-10-2026 07.56",
                    dateSubtitle = "01/10/2026 07:56  |  📄 1",
                    isExcel = false,
                    thumbnailPath = null,
                    isChecked = false,
                    onCheckedChange = {},
                    onClick = onScanClick
                )
            }
        } else {
            // First item: Highlighted / Expanded Card
            item {
                val firstDoc = documents.first()
                val isChecked = checkedMap[firstDoc.id] ?: false
                val dateStr = remember(firstDoc.createdTimestamp) {
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(firstDoc.createdTimestamp))
                }

                FeaturedDocumentCard(
                    title = firstDoc.title,
                    dateSubtitle = "$dateStr  |  📄 ${firstDoc.pageCount}",
                    thumbnailPath = firstDoc.thumbnailPath,
                    isChecked = isChecked,
                    onCheckedChange = { checkedMap = checkedMap + (firstDoc.id to it) },
                    onShareClick = { onShareClick(firstDoc) },
                    onWordClick = { onOcrClick(firstDoc) },
                    onViewClick = { onDocumentClick(firstDoc) }
                )
            }

            // Remaining items: Standard Document Rows
            itemsIndexed(documents.drop(1)) { _, doc ->
                val isChecked = checkedMap[doc.id] ?: false
                val dateStr = remember(doc.createdTimestamp) {
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(doc.createdTimestamp))
                }

                StandardDocumentRow(
                    title = doc.title,
                    dateSubtitle = "$dateStr  |  📄 ${doc.pageCount}",
                    isExcel = doc.category.contains("Excel", ignoreCase = true) || doc.category.contains("Sheet", ignoreCase = true),
                    thumbnailPath = doc.thumbnailPath,
                    isChecked = isChecked,
                    onCheckedChange = { checkedMap = checkedMap + (doc.id to it) },
                    onClick = { onDocumentClick(doc) }
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    circleColor: Color,
    iconColor: Color,
    hasFreeBadge: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = CircleShape,
                color = circleColor,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            if (hasFreeBadge) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFEF4444),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-2).dp)
                ) {
                    Text(
                        text = "Free",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF334155),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FeaturedDocumentCard(
    title: String,
    dateSubtitle: String,
    thumbnailPath: String?,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onShareClick: () -> Unit,
    onWordClick: () -> Unit,
    onViewClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onViewClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                DocumentThumbnail(thumbnailPath = thumbnailPath)

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }

                // Square Checkbox
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00A884))
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Action Buttons matching CamScanner: [ Bagikan ]  [ ✨ Ke Word ]  [ Lihat ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PillActionButton(
                    text = "Bagikan",
                    icon = null,
                    modifier = Modifier.weight(1f),
                    onClick = onShareClick
                )

                PillActionButton(
                    text = "Ke Word",
                    icon = Icons.Default.AutoAwesome,
                    iconColor = Color(0xFF0D9488),
                    modifier = Modifier.weight(1.2f),
                    onClick = onWordClick
                )

                PillActionButton(
                    text = "Lihat",
                    icon = null,
                    modifier = Modifier.weight(1f),
                    onClick = onViewClick
                )
            }
        }
    }
}

@Composable
fun StandardDocumentRow(
    title: String,
    dateSubtitle: String,
    isExcel: Boolean,
    thumbnailPath: String?,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isExcel) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Excel",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            } else {
                DocumentThumbnail(thumbnailPath = thumbnailPath)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }

            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00A884))
            )
        }
    }
}

@Composable
fun DocumentThumbnail(thumbnailPath: String?) {
    val bitmap = remember(thumbnailPath) {
        if (!thumbnailPath.isNullOrBlank()) {
            val file = File(thumbnailPath)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        } else null
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    } else {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF1F5F9),
            border = BorderStroke(0.6.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
fun PillActionButton(
    text: String,
    icon: ImageVector? = null,
    iconColor: Color = Color(0xFF00A884),
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF3F4F6),
        modifier = modifier
            .height(36.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF334155),
                fontSize = 12.sp
            )
        }
    }
}
