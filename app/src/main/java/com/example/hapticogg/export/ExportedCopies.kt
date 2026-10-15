package com.example.hapticogg.export

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore

/**
 * The copies of a converted file that the app exported earlier: entries in the music and
 * ringtone collections, and documents inside the folder the user picked. They are used to
 * show whether a local file still has an exported copy, and to delete that copy together
 * with the record.
 */
class ExportedCopies(private val context: Context) {

    /** Display names this app exported into its own music / ringtone folders. */
    fun ownNames(): Set<String> {
        val names = mutableSetOf<String>()
        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
                arrayOf(
                    "${ExportSpec.MUSIC_COLLECTION}/${ExportSpec.MUSIC_DIR}%",
                    "${ExportSpec.RINGTONE_COLLECTION}/${ExportSpec.MUSIC_DIR}%"
                ),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) cursor.getString(0)?.let(names::add)
            }
        } catch (_: Exception) {
            // a stale media database must not break the list
        }
        return names
    }

    /** Display names inside the folder the user picked. */
    fun folderNames(treeUri: Uri?): Set<String> {
        if (treeUri == null) return emptySet()
        val names = mutableSetOf<String>()
        try {
            context.contentResolver.query(
                DocumentTrees.children(treeUri),
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) cursor.getString(0)?.let(names::add)
            }
        } catch (_: Exception) {
            // folder may no longer be granted
        }
        return names
    }

    /**
     * Media library entries and picked-folder documents for this conversion. Older local
     * copies were stored under a sanitised name, so the timestamp suffix is matched too.
     */
    fun find(displayName: String, treeUri: Uri?): List<Uri> {
        val found = mutableListOf<Uri>()
        val suffix = timestampSuffix(displayName)
        try {
            val selection = if (suffix != null) {
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ? OR ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
            } else {
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
            }
            val args = if (suffix != null) arrayOf(displayName, "%$suffix") else arrayOf(displayName)
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.MediaColumns._ID),
                selection,
                args,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    found.add(
                        ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(0)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // ignore and continue with the folder
        }
        if (treeUri != null) {
            try {
                context.contentResolver.query(
                    DocumentTrees.children(treeUri),
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(1) ?: continue
                        if (name == displayName || (suffix != null && name.endsWith(suffix))) {
                            found.add(
                                DocumentsContract.buildDocumentUriUsingTree(treeUri, cursor.getString(0))
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // ignore
            }
        }
        return found
    }

    /** @return how many exported files were actually removed. */
    fun delete(uris: List<Uri>): Int {
        var removed = 0
        uris.forEach { uri ->
            try {
                if (context.contentResolver.delete(uri, null, null) > 0) removed++
            } catch (_: Exception) {
                // entries owned by another app cannot be removed
            }
        }
        return removed
    }

    /** "_yyyyMMdd_HHmmss.ogg" of a converted file name, used to find its exported copies. */
    private fun timestampSuffix(displayName: String): String? =
        Regex("_\\d{8}_\\d{6}\\.ogg$").find(displayName)?.value
}
