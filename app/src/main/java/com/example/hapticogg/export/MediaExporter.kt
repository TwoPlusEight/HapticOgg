package com.example.hapticogg.export

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File

/** Writes converted OGG files to the public music collection or to a chosen folder. */
class MediaExporter(private val context: Context) {

    /** Music/[ExportSpec.MUSIC_DIR] through MediaStore on API 29+, the public dir below that. */
    fun toMusicLibrary(source: File, displayName: String): Uri {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                ExportSpec.MUSIC_DIR
            )
            if (!dir.exists() && !dir.mkdirs()) throw IllegalStateException("无法创建 ${dir.absolutePath}")
            val target = uniqueFile(File(dir, displayName))
            source.inputStream().use { input -> target.outputStream().use { input.copyTo(it) } }
            MediaScannerConnection.scanFile(
                context, arrayOf(target.absolutePath), arrayOf(ExportSpec.MIME), null
            )
            return Uri.fromFile(target)
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, ExportSpec.MIME)
            // MediaStore only accepts the standard collections as the first path segment.
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${ExportSpec.MUSIC_COLLECTION}/${ExportSpec.MUSIC_DIR}/"
            )
            put(MediaStore.Audio.AudioColumns.IS_MUSIC, 1)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("无法在媒体库中创建文件")
        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                source.inputStream().use { it.copyTo(out) }
            } ?: throw IllegalStateException("无法写入媒体库")
        } catch (e: Exception) {
            context.contentResolver.delete(uri, null, null)
            throw e
        }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)
        return uri
    }

    /** Creates the file inside the SAF folder the user picked. */
    fun toFolder(source: File, treeUri: Uri, displayName: String): Uri {
        val parent = DocumentTrees.folder(treeUri)
        val name = uniqueName(treeUri, displayName)
        val document = DocumentsContract.createDocument(
            context.contentResolver, parent, ExportSpec.MIME, name
        ) ?: throw IllegalStateException("无法在所选文件夹中创建文件")
        context.contentResolver.openOutputStream(document, "wt")?.use { out ->
            source.inputStream().use { it.copyTo(out) }
        } ?: throw IllegalStateException("无法写入所选文件夹")
        return document
    }

    /** Folder name to show in the UI. */
    fun describeTree(treeUri: Uri): String {
        try {
            context.contentResolver.query(
                DocumentTrees.folder(treeUri),
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0)
                    if (!name.isNullOrBlank()) return name
                }
            }
        } catch (_: Exception) {
            // fall through to the URI based name
        }
        return treeUri.lastPathSegment?.substringAfterLast(':') ?: treeUri.toString()
    }

    private fun uniqueName(treeUri: Uri, displayName: String): String {
        val taken = mutableSetOf<String>()
        try {
            context.contentResolver.query(
                DocumentTrees.children(treeUri),
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) cursor.getString(0)?.let(taken::add)
            }
        } catch (_: Exception) {
            return displayName
        }
        if (displayName !in taken) return displayName
        val base = displayName.substringBeforeLast('.')
        val suffix = displayName.substringAfterLast('.', "")
        var index = 2
        while (true) {
            val candidate = if (suffix.isEmpty()) "${base}_$index" else "${base}_$index.$suffix"
            if (candidate !in taken) return candidate
            index++
        }
    }

    private fun uniqueFile(target: File): File {
        if (!target.exists()) return target
        val base = target.name.substringBeforeLast('.')
        val suffix = target.name.substringAfterLast('.', "")
        var index = 2
        while (true) {
            val candidate = File(
                target.parentFile,
                if (suffix.isEmpty()) "${base}_$index" else "${base}_$index.$suffix"
            )
            if (!candidate.exists()) return candidate
            index++
        }
    }
}
