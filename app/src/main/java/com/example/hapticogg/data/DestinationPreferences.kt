package com.example.hapticogg.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import java.io.File

/** Where converted OGGs are written. */
enum class DestinationMode { MUSIC, FOLDER }

/**
 * Destination choice, the one-time consent answer and the last conversion, kept in
 * SharedPreferences so they survive process restarts.
 */
class DestinationPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var mode: DestinationMode
        get() = if (prefs.getString(KEY_MODE, MODE_MUSIC) == MODE_FOLDER) {
            DestinationMode.FOLDER
        } else {
            DestinationMode.MUSIC
        }
        set(value) {
            val stored = if (value == DestinationMode.FOLDER) MODE_FOLDER else MODE_MUSIC
            prefs.edit().putString(KEY_MODE, stored).apply()
        }

    /** Folder picked through the system picker; null until one was ever picked. */
    var treeUri: Uri?
        get() = prefs.getString(KEY_TREE, null)?.let(Uri::parse)
        set(value) {
            prefs.edit().putString(KEY_TREE, value?.toString()).apply()
        }

    /** True once the user answered the "save to the music directory?" question. */
    var consentAnswered: Boolean
        get() = prefs.getBoolean(KEY_CONSENT, false)
        set(value) {
            prefs.edit().putBoolean(KEY_CONSENT, value).apply()
        }

    /**
     * Local copy of the most recent successful conversion. The file can disappear behind
     * the app's back, so callers check [File.exists] before using it.
     */
    var lastOutput: File?
        get() = prefs.getString(KEY_LAST_FILE, null)?.let(::File)
        set(value) {
            val editor = prefs.edit()
            if (value == null) {
                editor.remove(KEY_LAST_FILE)
            } else {
                editor.putString(KEY_LAST_FILE, value.absolutePath)
            }
            editor.apply()
        }

    private companion object {
        const val PREFS = "haptic_ogg"
        const val KEY_MODE = "dest_mode"
        const val KEY_TREE = "dest_tree"
        const val KEY_CONSENT = "dest_consent"
        const val KEY_LAST_FILE = "last_file"
        const val MODE_MUSIC = "music"
        const val MODE_FOLDER = "folder"
    }
}
