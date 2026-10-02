package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionType: String, // SCAN, CONVERT, COMPRESS, EDIT, READ, DELETE, SHARE
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val documentId: Long? = null
)
