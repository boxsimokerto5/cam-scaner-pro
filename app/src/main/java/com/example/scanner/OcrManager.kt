package com.example.scanner

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OcrManager(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Extracts text from multiple page URIs on-device (100% offline)
     */
    suspend fun extractTextFromPages(pageUris: List<Uri>): String = withContext(Dispatchers.Default) {
        val stringBuilder = StringBuilder()

        for ((index, uri) in pageUris.withIndex()) {
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val visionText = recognizer.process(inputImage).awaitResult()
                val text = visionText.text.trim()
                if (text.isNotEmpty()) {
                    if (pageUris.size > 1) {
                        stringBuilder.append("[Halaman ${index + 1}]\n")
                    }
                    stringBuilder.append(text)
                    stringBuilder.append("\n\n")
                }
            } catch (e: Exception) {
                // Skip unreadable page gracefully
            }
        }

        stringBuilder.toString().trim()
    }
}

/**
 * Await extension for Google Play Services Tasks
 */
suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) {
            continuation.resume(result)
        }
    }
    addOnFailureListener { exception ->
        if (continuation.isActive) {
            continuation.resumeWithException(exception)
        }
    }
    addOnCanceledListener {
        if (continuation.isActive) {
            continuation.cancel()
        }
    }
}
