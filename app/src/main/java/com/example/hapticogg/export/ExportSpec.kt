package com.example.hapticogg.export

/** Names and MIME types of everything the app writes outside its own storage. */
object ExportSpec {
    /** MIME type of the converted files. */
    const val MIME = "audio/ogg"

    /** Sub folder created inside the public music and ringtone collections. */
    const val MUSIC_DIR = "HapticOgg"

    /** MediaStore collections: only the standard ones are accepted as first path segment. */
    internal const val MUSIC_COLLECTION = "Music"
    internal const val RINGTONE_COLLECTION = "Ringtones"
}
