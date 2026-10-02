package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String = "Umum",
    val createdTimestamp: Long = System.currentTimeMillis(),
    val pageCount: Int,
    val thumbnailPath: String,
    val pdfPath: String,
    val ocrText: String = ""
)
