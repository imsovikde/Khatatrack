package com.example.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream

/**
 * AttachmentManager — handles persisting attachment files to internal app storage.
 *
 * Android content:// URIs from camera/gallery are temporary — they become invalid
 * after the app is killed. This utility copies the file to app-internal storage
 * and returns a file:// URI that is permanently accessible.
 *
 * Call `persist()` when the user selects/captures an image or PDF.
 * The returned URI is what should be saved to the database.
 */
object AttachmentManager {

    private const val ATTACHMENTS_DIR = "khata_attachments"

    /**
     * Persists any content:// or file:// URI to app-internal storage.
     * Returns a persistent file:// URI string, or null on failure.
     */
    fun persist(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = getAttachmentsDir(context)
            val mimeType = context.contentResolver.getType(sourceUri)
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
            val fileName = "att_${System.currentTimeMillis()}.$ext"
            val destFile = File(dir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return null

            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Deletes a persisted attachment file.
     * Pass the stored URI string from the database.
     */
    fun delete(uriString: String?) {
        if (uriString.isNullOrBlank()) return
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                File(uri.path ?: return).delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Returns true if the URI points to a persisted internal file (safe to display).
     * Returns false for content:// URIs that may be stale.
     */
    fun isPersisted(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file") {
            return File(uri.path ?: return false).exists()
        }
        return false
    }

    /**
     * Resolves an attachment URI for display — if the file:// exists, returns it.
     * If it's a stale content:// URI, returns null so UI shows blank gracefully.
     */
    fun resolveForDisplay(uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        val uri = Uri.parse(uriString)
        return when (uri.scheme) {
            "file" -> if (File(uri.path ?: return null).exists()) uriString else null
            "content" -> uriString // still try — Coil/Glide handle the error gracefully
            else -> null
        }
    }

    /**
     * Total size of all persisted attachments in bytes.
     */
    fun totalAttachmentSize(context: Context): Long {
        return getAttachmentsDir(context).listFiles()?.sumOf { it.length() } ?: 0L
    }

    private fun getAttachmentsDir(context: Context): File {
        val dir = File(context.filesDir, ATTACHMENTS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
