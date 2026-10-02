package com.example.data.repository

import android.net.Uri
import com.example.data.local.DocumentDao
import com.example.data.local.DocumentEntity
import com.example.data.local.HistoryDao
import com.example.data.local.HistoryEntity
import com.example.data.storage.FileStorageManager
import com.example.scanner.OcrManager
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class DocumentRepository(
    private val documentDao: DocumentDao,
    private val historyDao: HistoryDao,
    private val storageManager: FileStorageManager,
    private val ocrManager: OcrManager
) {
    fun getAllDocuments(): Flow<List<DocumentEntity>> = documentDao.getAllDocuments()

    fun getDocumentsByCategory(category: String): Flow<List<DocumentEntity>> =
        if (category == "Semua") documentDao.getAllDocuments()
        else documentDao.getDocumentsByCategory(category)

    fun searchDocuments(query: String, category: String): Flow<List<DocumentEntity>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            if (category == "Semua") documentDao.getAllDocuments()
            else documentDao.getDocumentsByCategory(category)
        } else {
            if (category == "Semua") documentDao.searchDocuments(trimmed)
            else documentDao.searchDocumentsWithCategory(trimmed, category)
        }
    }

    suspend fun processAndSaveScanResult(
        result: GmsDocumentScanningResult,
        customTitle: String = "Dokumen Baru",
        category: String = "Umum"
    ): DocumentEntity? = withContext(Dispatchers.IO) {
        val pdfUri = result.pdf?.uri ?: return@withContext null
        val pages = result.pages ?: return@withContext null
        val firstPageUri = pages.firstOrNull()?.imageUri ?: return@withContext null

        val finalTitle = customTitle.ifBlank { "Scan_${System.currentTimeMillis() % 10000}" }

        // 1. Save PDF file to internal storage
        val savedPdf = storageManager.savePdfFromUri(pdfUri, prefix = finalTitle)

        // 2. Save thumbnail
        val thumbFile = storageManager.saveThumbnailFromUri(firstPageUri, savedPdf.nameWithoutExtension)

        // 3. Perform on-device OCR
        val pageUris = pages.mapNotNull { it.imageUri }
        val ocrResult = ocrManager.extractTextFromPages(pageUris)

        // 4. Save to Room database
        val entity = DocumentEntity(
            title = finalTitle,
            category = category,
            pageCount = pages.size,
            thumbnailPath = thumbFile.absolutePath,
            pdfPath = savedPdf.absolutePath,
            ocrText = ocrResult
        )

        val id = documentDao.insertDocument(entity)
        logActivity(
            actionType = "SCAN",
            title = "Pemindaian Kamera",
            description = "Memindai dokumen '$finalTitle' (${pages.size} halaman) dengan OCR",
            docId = id
        )
        entity.copy(id = id)
    }

    suspend fun processAndSaveGalleryImages(
        uris: List<Uri>,
        customTitle: String = "Impor Galeri",
        category: String = "Umum"
    ): DocumentEntity? = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext null

        val finalTitle = customTitle.ifBlank { "Galeri_${System.currentTimeMillis() % 10000}" }

        // 1. Create PDF and Thumbnail
        val (pdfFile, thumbFile) = storageManager.createPdfFromImageUris(uris, finalTitle)

        // 2. Perform on-device OCR
        val ocrResult = ocrManager.extractTextFromPages(uris)

        // 3. Save to Room
        val entity = DocumentEntity(
            title = finalTitle,
            category = category,
            pageCount = uris.size,
            thumbnailPath = thumbFile.absolutePath,
            pdfPath = pdfFile.absolutePath,
            ocrText = ocrResult
        )

        val id = documentDao.insertDocument(entity)
        logActivity(
            actionType = "CONVERT",
            title = "Impor Foto ke PDF",
            description = "Mengonversi ${uris.size} foto menjadi PDF '$finalTitle'",
            docId = id
        )
        entity.copy(id = id)
    }

    suspend fun updateDocument(document: DocumentEntity) = withContext(Dispatchers.IO) {
        documentDao.updateDocument(document)
        logActivity(
            actionType = "EDIT",
            title = "Pembaruan Dokumen",
            description = "Memperbarui info '${document.title}' (${document.category})",
            docId = document.id
        )
    }

    suspend fun insertCustomDocument(document: DocumentEntity): Long = withContext(Dispatchers.IO) {
        val id = documentDao.insertDocument(document)
        logActivity(
            actionType = when {
                document.title.contains("Terkecil", ignoreCase = true) -> "COMPRESS"
                document.title.contains("Edited", ignoreCase = true) || document.title.contains("+") -> "EDIT"
                else -> "CONVERT"
            },
            title = document.title,
            description = "Menyimpan dokumen ${document.pageCount} halaman (${document.category})",
            docId = id
        )
        id
    }

    suspend fun deleteDocument(document: DocumentEntity) = withContext(Dispatchers.IO) {
        storageManager.deleteDocumentFiles(document.pdfPath, document.thumbnailPath)
        documentDao.deleteDocument(document)
        logActivity(
            actionType = "DELETE",
            title = "Hapus Dokumen",
            description = "Menghapus berkas dokumen '${document.title}'",
            docId = document.id
        )
    }

    fun getShareablePdfUri(pdfPath: String): Uri {
        return storageManager.getShareableUri(File(pdfPath))
    }

    suspend fun logActivity(
        actionType: String,
        title: String,
        description: String,
        docId: Long? = null
    ) = withContext(Dispatchers.IO) {
        historyDao.insertHistory(
            HistoryEntity(
                actionType = actionType,
                title = title,
                description = description,
                timestamp = System.currentTimeMillis(),
                documentId = docId
            )
        )
    }

    fun getAllHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        historyDao.clearAllHistory()
    }
}
