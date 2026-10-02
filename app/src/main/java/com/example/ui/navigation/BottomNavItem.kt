package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    HOME(
        title = "Beranda",
        icon = Icons.Default.Home,
        testTag = "tab_home"
    ),
    DOCUMENTS(
        title = "Semua Doku...",
        icon = Icons.Default.Article,
        testTag = "tab_all_docs"
    ),
    TOOLS(
        title = "Alat",
        icon = Icons.Default.Widgets,
        testTag = "tab_tools"
    ),
    PROFILE(
        title = "Saya",
        icon = Icons.Default.Person,
        testTag = "tab_profile"
    )
}
