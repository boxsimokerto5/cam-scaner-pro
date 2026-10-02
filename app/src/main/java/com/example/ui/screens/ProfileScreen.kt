package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.DocumentEntity
import com.example.security.BiometricAuthManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    documents: List<DocumentEntity>,
    biometricManager: BiometricAuthManager,
    onLockApp: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isBiometricEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(bottom = 90.dp)
    ) {
        // User Profile Card Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar with EDU badge
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF00A884).copy(alpha = 0.15f),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF00A884),
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        // Red EDU Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-2).dp)
                        ) {
                            Text(
                                text = "EDU",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Pengguna Pro",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = "VIP",
                                    color = Color(0xFFD97706),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Lisensi Edukasi Aktif • Bebas Iklan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = {
                        Toast.makeText(context, "Profil sudah tersinkronisasi", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.QrCode, contentDescription = "Kode QR Saya")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStatItem(title = "Dokumen", count = "${documents.size}")
                    ProfileStatItem(title = "Halaman", count = "${documents.sumOf { it.pageCount }}")
                    ProfileStatItem(title = "Penyimpanan", count = "Lokal (Aman)")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security Section
        SectionCard(title = "Keamanan & Privasi") {
            SettingsRow(
                icon = Icons.Default.Fingerprint,
                title = "Kunci Aplikasi & Biometrik",
                subtitle = "Lindungi dokumen dengan sidik jari / pola",
                trailing = {
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { isBiometricEnabled = it }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            SettingsRow(
                icon = Icons.Default.Lock,
                title = "Kunci Aplikasi Sekarang",
                subtitle = "Langsung sembunyikan dokumen di layar",
                onClick = onLockApp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Document Management Section
        SectionCard(title = "Manajemen Dokumen") {
            SettingsRow(
                icon = Icons.Default.FolderZip,
                title = "Kualitas Kompresi Default",
                subtitle = "Sedang (Rekomendasi - hemat ~45%)",
                onClick = { Toast.makeText(context, "Diatur ke: Kualitas Sedang", Toast.LENGTH_SHORT).show() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            SettingsRow(
                icon = Icons.Default.CleaningServices,
                title = "Bersihkan Cache & Temp",
                subtitle = "Kosongkan berkas pemrosesan sementara",
                onClick = {
                    context.cacheDir.deleteRecursively()
                    Toast.makeText(context, "Cache berhasil dibersihkan!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About Application Section
        SectionCard(title = "Tentang Aplikasi") {
            SettingsRow(
                icon = Icons.Default.Info,
                title = "Versi Aplikasi",
                subtitle = "v1.0 (Build 1) • com.camscanerpro.gecckocreator"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            SettingsRow(
                icon = Icons.Default.Shield,
                title = "100% Offline & Privat",
                subtitle = "Semua proses OCR dan PDF berjalan di HP tanpa server luar"
            )
        }
    }
}

@Composable
fun ProfileStatItem(title: String, count: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}
