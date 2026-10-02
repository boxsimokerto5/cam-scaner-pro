package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    DOCUMENTS(
        title = "Dokumen",
        icon = Icons.Default.Description,
        testTag = "tab_documents"
    ),
    CONVERT(
        title = "Convert",
        icon = Icons.Default.SwapHoriz,
        testTag = "tab_convert"
    ),
    COMPRESS(
        title = "Kompres",
        icon = Icons.Default.FolderZip,
        testTag = "tab_compress"
    ),
    READER(
        title = "Reader",
        icon = Icons.Default.MenuBook,
        testTag = "tab_reader"
    ),
    EDIT_PDF(
        title = "Edit PDF",
        icon = Icons.Default.Tune,
        testTag = "tab_edit_pdf"
    ),
    HISTORY(
        title = "History",
        icon = Icons.Default.History,
        testTag = "tab_history"
    )
}
