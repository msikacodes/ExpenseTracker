package com.example.myapplication.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File

/**
 * Copies a temporary URI (e.g. from gallery pickers) to internal storage for persistent access.
 */
fun copyUriToInternalStorage(context: Context, uriString: String): String? {
    val uri = Uri.parse(uriString) ?: return null
    return try {
        // If it's already a file URI in our app's storage, no need to copy again.
        if (uri.scheme == "file" && uri.path?.contains(context.packageName) == true) {
            return uriString
        }
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(
            context.contentResolver.getType(uri)
        ) ?: "jpg"
        
        val receiptsDir = File(context.filesDir, "receipts")
        if (!receiptsDir.exists()) {
            receiptsDir.mkdirs()
        }
        val file = File(receiptsDir, "receipt_${System.currentTimeMillis()}.$extension")
        file.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
