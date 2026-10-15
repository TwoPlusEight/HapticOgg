package com.example.hapticogg.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

/** Reads the audio files the user picked through the system file picker. */
class ContentUriFiles(private val context: Context) {

    /** Name the provider reports for [uri], falling back to the URI itself. */
    fun displayName(uri: Uri): String {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val name = cursor.getString(0)
                        if (!name.isNullOrBlank()) return name
                    }
                }
        } catch (_: Exception) {
            // fall back to the URI
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: FALLBACK_NAME
    }

    /** Copies [uri] into the cache directory so FFmpeg can read it as a plain file. */
    fun copyToCache(uri: Uri, sourceName: String): File {
        val file = File(context.cacheDir, "src_${System.nanoTime()}_${cacheName(sourceName)}")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法读取所选文件" }
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        return file
    }

    private fun cacheName(sourceName: String): String =
        sourceName.replace(UNSAFE_NAME, "_").take(80).ifBlank { FALLBACK_NAME }

    private companion object {
        const val FALLBACK_NAME = "audio"
        val UNSAFE_NAME = Regex("[^A-Za-z0-9._-]")
    }
}
