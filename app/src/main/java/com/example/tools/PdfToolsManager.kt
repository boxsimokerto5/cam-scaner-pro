package com.example.tools

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfToolsManager(private val context: Context) {

    private val toolsDir: File by lazy {
        File(context.filesDir, "scanned_documents").apply {
            if (!exists()) mkdirs()
        }
    }

    private val thumbsDir: File by lazy {
        File(context.filesDir, "thumbnails").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Compresses a PDF file with real-time percentage and status message callbacks
     */
    suspend fun compressPdf(
        sourcePdf: File,
        scaleFactor: Float = 0.75f,
        jpegQuality: Int = 70,
        onProgress: (suspend (Float, String) -> Unit)? = null
    ): File = withContext(Dispatchers.IO) {
        onProgress?.invoke(0.05f, "Mempersiapkan dokumen dan membuka berkas...")

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outFile = File(toolsDir, "${sourcePdf.nameWithoutExtension}_compressed_$timeStamp.pdf")

        val pfd = ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val totalPages = renderer.pageCount
        val newPdf = PdfDocument()

        try {
            for (i in 0 until totalPages) {
                val currentProgress = 0.10f + (0.75f * i / totalPages.coerceAtLeast(1))
                onProgress?.invoke(
                    currentProgress,
                    "Membaca & mengoptimalkan halaman ${i + 1} dari $totalPages..."
                )

                val page = renderer.openPage(i)
                val targetW = (page.width * scaleFactor).toInt().coerceAtLeast(200)
                val targetH = (page.height * scaleFactor).toInt().coerceAtLeast(300)

                val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val stream = java.io.ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, jpegQuality, stream)
                val byteArray = stream.toByteArray()
                val compressedBmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size) ?: bmp

                val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
                val newPage = newPdf.startPage(pageInfo)
                val srcRect = Rect(0, 0, compressedBmp.width, compressedBmp.height)
                val destRect = Rect(0, 0, page.width, page.height)
                newPage.canvas.drawBitmap(compressedBmp, srcRect, destRect, null)
                newPdf.finishPage(newPage)

                if (compressedBmp !== bmp) compressedBmp.recycle()
                bmp.recycle()
            }

            onProgress?.invoke(0.90f, "Menyusun dan menulis struktur file PDF baru...")
            FileOutputStream(outFile).use { out ->
                newPdf.writeTo(out)
            }
            onProgress?.invoke(1.0f, "Kompresi selesai! Menyiapkan hasil...")
        } finally {
            newPdf.close()
            renderer.close()
            pfd.close()
        }

        outFile
    }

    /**
     * Converts formatted plain text into a multi-page PDF with progress updates
     */
    suspend fun convertTextToPdf(
        title: String,
        bodyText: String,
        onProgress: (suspend (Float, String) -> Unit)? = null
    ): Pair<File, File> = withContext(Dispatchers.IO) {
        onProgress?.invoke(0.10f, "Memformat dan menghitung tata letak paragraf teks...")

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Teks" }.take(30)
        val outFile = File(toolsDir, "${safeTitle}_$timeStamp.pdf")
        val thumbFile = File(thumbsDir, "thumb_${safeTitle}_$timeStamp.jpg")

        val pageWidth = 595 // A4 standard width in points (72 dpi)
        val pageHeight = 842 // A4 standard height in points
        val margin = 50f
        val printableWidth = pageWidth - (margin * 2)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 12f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val pdfDocument = PdfDocument()

        val lines = mutableListOf<String>()
        val paragraphs = bodyText.split("\n")
        for (paragraph in paragraphs) {
            if (paragraph.isBlank()) {
                lines.add("")
                continue
            }
            val words = paragraph.split(" ")
            var currentLine = ""
            for (word in words) {
                val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (textPaint.measureText(candidate) <= printableWidth) {
                    currentLine = candidate
                } else {
                    if (currentLine.isNotEmpty()) lines.add(currentLine)
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) lines.add(currentLine)
        }

        val lineHeight = 18f
        val contentStartY = margin + 40f
        val linesPerPage = ((pageHeight - margin - contentStartY) / lineHeight).toInt().coerceAtLeast(1)

        var pageIndex = 0
        var currentLineIndex = 0

        onProgress?.invoke(0.35f, "Menyusun lembar dan nomor halaman dokumen...")

        while (currentLineIndex < lines.size || pageIndex == 0) {
            val progress = 0.35f + (0.45f * (currentLineIndex.toFloat() / lines.size.coerceAtLeast(1)))
            onProgress?.invoke(progress, "Menulis isi teks ke halaman ${pageIndex + 1}...")

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)

            var yOffset = margin
            if (pageIndex == 0) {
                canvas.drawText(title, margin, yOffset + 20f, titlePaint)
                canvas.drawLine(margin, yOffset + 32f, pageWidth - margin, yOffset + 32f, Paint().apply {
                    color = Color.parseColor("#CBD5E1")
                    strokeWidth = 1f
                })
                yOffset = contentStartY
            } else {
                yOffset = margin + 20f
            }

            var linesDrawn = 0
            while (currentLineIndex < lines.size && linesDrawn < linesPerPage) {
                val line = lines[currentLineIndex]
                canvas.drawText(line, margin, yOffset + (linesDrawn * lineHeight), textPaint)
                linesDrawn++
                currentLineIndex++
            }

            val footerPaint = Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = 10f
                isAntiAlias = true
            }
            val footerText = "Hal ${pageIndex + 1}"
            canvas.drawText(footerText, pageWidth - margin - 30f, pageHeight - 30f, footerPaint)

            pdfDocument.finishPage(page)
            pageIndex++

            if (currentLineIndex >= lines.size) break
        }

        onProgress?.invoke(0.85f, "Menyimpan file dokumen PDF...")
        FileOutputStream(outFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        onProgress?.invoke(0.95f, "Membuat thumbnail pratinjau...")
        val pfd = ParcelFileDescriptor.open(outFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        if (renderer.pageCount > 0) {
            val firstPage = renderer.openPage(0)
            val thumbBmp = Bitmap.createBitmap(firstPage.width, firstPage.height, Bitmap.Config.ARGB_8888)
            firstPage.render(thumbBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            firstPage.close()
            FileOutputStream(thumbFile).use { out ->
                thumbBmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            thumbBmp.recycle()
        }
        renderer.close()
        pfd.close()

        onProgress?.invoke(1.0f, "Dokumen PDF teks berhasil dibuat!")
        Pair(outFile, thumbFile)
    }

    /**
     * Edits pages with rotation, deletion, and watermark with progress reporting
     */
    suspend fun editPdf(
        sourcePdf: File,
        pagesToKeep: List<Int>,
        rotations: Map<Int, Int>,
        watermarkText: String? = null,
        onProgress: (suspend (Float, String) -> Unit)? = null
    ): Pair<File, File> = withContext(Dispatchers.IO) {
        onProgress?.invoke(0.08f, "Membuka file PDF dan membaca struktur halaman...")

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outFile = File(toolsDir, "${sourcePdf.nameWithoutExtension}_edited_$timeStamp.pdf")
        val thumbFile = File(thumbsDir, "thumb_${sourcePdf.nameWithoutExtension}_edited_$timeStamp.jpg")

        val pfd = ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val newPdf = PdfDocument()

        val watermarkPaint = Paint().apply {
            color = Color.argb(45, 2, 132, 199)
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        try {
            var newPageNumber = 1
            val totalKept = pagesToKeep.size
            for ((idx, pageIndex) in pagesToKeep.withIndex()) {
                if (pageIndex !in 0 until renderer.pageCount) continue
                val progress = 0.10f + (0.75f * idx / totalKept.coerceAtLeast(1))
                onProgress?.invoke(progress, "Memproses & menata halaman ${idx + 1} dari $totalKept...")

                val page = renderer.openPage(pageIndex)
                val rotationDegrees = rotations[pageIndex] ?: 0

                val isQuarterTurn = rotationDegrees == 90 || rotationDegrees == 270
                val targetW = if (isQuarterTurn) page.height else page.width
                val targetH = if (isQuarterTurn) page.width else page.height

                val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pageInfo = PdfDocument.PageInfo.Builder(targetW, targetH, newPageNumber).create()
                val newPage = newPdf.startPage(pageInfo)
                val canvas = newPage.canvas

                canvas.drawColor(Color.WHITE)

                canvas.save()
                when (rotationDegrees) {
                    90 -> {
                        canvas.translate(targetW.toFloat(), 0f)
                        canvas.rotate(90f)
                    }
                    180 -> {
                        canvas.translate(targetW.toFloat(), targetH.toFloat())
                        canvas.rotate(180f)
                    }
                    270 -> {
                        canvas.translate(0f, targetH.toFloat())
                        canvas.rotate(270f)
                    }
                }
                canvas.drawBitmap(bmp, 0f, 0f, null)
                canvas.restore()

                if (!watermarkText.isNullOrBlank()) {
                    canvas.save()
                    canvas.rotate(-45f, targetW / 2f, targetH / 2f)
                    canvas.drawText(watermarkText, targetW / 2f, targetH / 2f, watermarkPaint)
                    canvas.restore()
                }

                newPdf.finishPage(newPage)
                bmp.recycle()
                newPageNumber++
            }

            onProgress?.invoke(0.90f, "Menyimpan file hasil edit...")
            FileOutputStream(outFile).use { out ->
                newPdf.writeTo(out)
            }
        } finally {
            newPdf.close()
            renderer.close()
            pfd.close()
        }

        onProgress?.invoke(0.96f, "Membuat thumbnail halaman baru...")
        val outPfd = ParcelFileDescriptor.open(outFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val outRenderer = PdfRenderer(outPfd)
        if (outRenderer.pageCount > 0) {
            val firstPage = outRenderer.openPage(0)
            val thumbBmp = Bitmap.createBitmap(firstPage.width, firstPage.height, Bitmap.Config.ARGB_8888)
            firstPage.render(thumbBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            firstPage.close()
            FileOutputStream(thumbFile).use { out ->
                thumbBmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            thumbBmp.recycle()
        }
        outRenderer.close()
        outPfd.close()

        onProgress?.invoke(1.0f, "Pembaruan PDF selesai!")
        Pair(outFile, thumbFile)
    }

    /**
     * Merges multiple PDF files with real-time progress updates
     */
    suspend fun mergePdfs(
        pdfFiles: List<File>,
        mergedTitle: String,
        onProgress: (suspend (Float, String) -> Unit)? = null
    ): Pair<File, File> = withContext(Dispatchers.IO) {
        onProgress?.invoke(0.08f, "Mempersiapkan dokumen yang akan digabung...")

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safeTitle = mergedTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Gabungan" }
        val outFile = File(toolsDir, "${safeTitle}_$timeStamp.pdf")
        val thumbFile = File(thumbsDir, "thumb_${safeTitle}_$timeStamp.jpg")

        val newPdf = PdfDocument()
        var globalPageNumber = 1
        val totalFiles = pdfFiles.size

        try {
            for ((fileIdx, file) in pdfFiles.withIndex()) {
                if (!file.exists()) continue
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                val pagesInDoc = renderer.pageCount

                for (i in 0 until pagesInDoc) {
                    val progress = 0.10f + (0.75f * (fileIdx + (i.toFloat() / pagesInDoc)) / totalFiles.coerceAtLeast(1))
                    onProgress?.invoke(
                        progress,
                        "Menggabungkan ${file.nameWithoutExtension}: Halaman ${i + 1} dari $pagesInDoc..."
                    )

                    val page = renderer.openPage(i)
                    val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    page.close()

                    val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, globalPageNumber).create()
                    val newPage = newPdf.startPage(pageInfo)
                    newPage.canvas.drawBitmap(bmp, 0f, 0f, null)
                    newPdf.finishPage(newPage)

                    bmp.recycle()
                    globalPageNumber++
                }

                renderer.close()
                pfd.close()
            }

            onProgress?.invoke(0.92f, "Menulis struktur berkas gabungan...")
            FileOutputStream(outFile).use { out ->
                newPdf.writeTo(out)
            }
        } finally {
            newPdf.close()
        }

        onProgress?.invoke(0.97f, "Membuat thumbnail pratinjau...")
        val outPfd = ParcelFileDescriptor.open(outFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val outRenderer = PdfRenderer(outPfd)
        if (outRenderer.pageCount > 0) {
            val firstPage = outRenderer.openPage(0)
            val thumbBmp = Bitmap.createBitmap(firstPage.width, firstPage.height, Bitmap.Config.ARGB_8888)
            firstPage.render(thumbBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            firstPage.close()
            FileOutputStream(thumbFile).use { out ->
                thumbBmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            thumbBmp.recycle()
        }
        outRenderer.close()
        outPfd.close()

        onProgress?.invoke(1.0f, "Penggabungan dokumen selesai!")
        Pair(outFile, thumbFile)
    }

    /**
     * Extracts pages as images with progress callback
     */
    suspend fun extractPagesAsImages(
        sourcePdf: File,
        onProgress: (suspend (Float, String) -> Unit)? = null
    ): List<File> = withContext(Dispatchers.IO) {
        onProgress?.invoke(0.08f, "Membuka file PDF...")

        val extractedImages = mutableListOf<File>()
        val pfd = ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val total = renderer.pageCount

        val outFolder = File(context.cacheDir, "extracted_${sourcePdf.nameWithoutExtension}").apply {
            if (!exists()) mkdirs()
        }

        try {
            for (i in 0 until total) {
                val progress = 0.10f + (0.85f * i / total.coerceAtLeast(1))
                onProgress?.invoke(progress, "Mengekstrak halaman ${i + 1} dari $total menjadi JPEG...")

                val page = renderer.openPage(i)
                val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val imageFile = File(outFolder, "halaman_${i + 1}.jpg")
                FileOutputStream(imageFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                bmp.recycle()
                extractedImages.add(imageFile)
            }
            onProgress?.invoke(1.0f, "Semua halaman berhasil diekstrak!")
        } finally {
            renderer.close()
            pfd.close()
        }

        extractedImages
    }

    suspend fun copyUriToTempFile(uri: Uri, fileName: String = "temp.pdf"): File = withContext(Dispatchers.IO) {
        val temp = File(context.cacheDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(temp).use { output ->
                input.copyTo(output)
            }
        }
        temp
    }
}
