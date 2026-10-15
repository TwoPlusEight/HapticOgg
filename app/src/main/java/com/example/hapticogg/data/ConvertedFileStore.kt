package com.example.hapticogg.data

import android.content.Context
import java.io.File

/** The app's own copies of converted files, kept in the app's external files directory. */
class ConvertedFileStore(private val context: Context) {

    /** Directory that holds the converted OGGs; created on demand. */
    val directory: File
        get() = (context.getExternalFilesDir(DIR_NAME) ?: File(context.filesDir, DIR_NAME))
            .apply { mkdirs() }

    /** File the converter writes to before the result is exported. */
    fun outputFile(name: String): File = File(directory, name)

    /** Converted OGGs, newest first. */
    fun listConverted(): List<File> {
        val dir = directory
        // Envelope files only backed the removed self-drawn vibration modes.
        dir.listFiles { file -> file.isFile && file.name.endsWith(ENVELOPE_SUFFIX, ignoreCase = true) }
            ?.forEach { it.delete() }
        return dir.listFiles { file -> file.isFile && file.name.endsWith(OGG_SUFFIX, ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /** Deletes [file] together with the envelope that belongs to it. @return files removed. */
    fun delete(file: File): Int {
        val envelope = File(file.parentFile, file.nameWithoutExtension + ENVELOPE_SUFFIX)
        val envelopeRemoved = if (envelope.exists() && envelope.delete()) 1 else 0
        val fileRemoved = if (file.delete()) 1 else 0
        return envelopeRemoved + fileRemoved
    }

    private companion object {
        const val DIR_NAME = "converted"
        const val OGG_SUFFIX = ".ogg"
        const val ENVELOPE_SUFFIX = ".env"
    }
}
