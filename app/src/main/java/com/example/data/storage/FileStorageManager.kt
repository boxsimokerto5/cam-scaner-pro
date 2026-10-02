package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileStorageManager(private val context: Context) {

    private val documentsDir: File by lazy {
        File(context.filesDir, "scanned_documents").apply {
            if (!exists()) mkdirs()
        }
    }

    private val thumbnailsDir: File by lazy {
        File(context.filesDir, "thumbnails").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Copies PDF from temporary ML Kit / content URI into internal app storage
     */
    suspend fun savePdfFromUri(tempUri: Uri, prefix: String = "DOC"): File = withContext(Dispatchers.IO) {
        val safePrefix = prefix.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val targetFile = File(documentsDir, "${safePrefix}_$timeStamp.pdf")

        context.contentResolver.openInputStream(tempUri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Cannot read input stream from Uri: $tempUri")

        targetFile
    }

    /**
     * Saves first page image as thumbnail
     */
    suspend fun saveThumbnailFromUri(imageUri: Uri, baseName: String): File = withContext(Dispatchers.IO) {
        val targetFile = File(thumbnailsDir, "thumb_${baseName}.jpg")
        context.contentResolver.openInputStream(imageUri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
        targetFile
    }

    /**
     * Saves a Bitmap directly as thumbnail
     */
    suspend fun saveThumbnailFromBitmap(bitmap: Bitmap, baseName: String): File = withContext(Dispatchers.IO) {
        val targetFile = File(thumbnailsDir, "thumb_${baseName}.jpg")
        FileOutputStream(targetFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
        }
        targetFile
    }

    /**
     * Converts a list of image Uris (e.g. from Photo Picker / Gallery) into a multi-page PDF
     * and saves it to internal documentsDir.
     */
    suspend fun createPdfFromImageUris(
        imageUris: List<Uri>,
        title: String
    ): Pair<File, File> = withContext(Dispatchers.IO) {
        val safePrefix = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "DOC" }.take(30)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val pdfFile = File(documentsDir, "${safePrefix}_$timeStamp.pdf")
        val thumbFile = File(thumbnailsDir, "thumb_${safePrefix}_$timeStamp.jpg")

        val pdfDocument = PdfDocument()
        var firstBitmapSaved = false

        try {
            imageUris.forEachIndexed { index, uri ->
                val bitmap = decodeSampledBitmapFromUri(uri, reqWidth = 1200, reqHeight = 1600)
                if (bitmap != null) {
                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(page)

                    if (!firstBitmapSaved) {
                        FileOutputStream(thumbFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        }
                        firstBitmapSaved = true
                    }
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            pdfDocument.close()
        }

        Pair(pdfFile, thumbFile)
    }

    private fun decodeSampledBitmapFromUri(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        var inSampleSize = 1
        val height = options.outHeight
        val width = options.outWidth

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inJustDecodeBounds = false
        }

        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    }

    /**
     * Deletes document files
     */
    suspend fun deleteDocumentFiles(pdfPath: String, thumbPath: String) = withContext(Dispatchers.IO) {
        val pdf = File(pdfPath)
        if (pdf.exists()) pdf.delete()
        val thumb = File(thumbPath)
        if (thumb.exists()) thumb.delete()
    }

    /**
     * Obtains secure shareable URI using FileProvider
     */
    fun getShareableUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
