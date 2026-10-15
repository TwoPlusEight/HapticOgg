package com.example.hapticogg.conversion

import android.net.Uri

/** Where a batch of converted files is written. */
sealed interface ConversionDestination {

    /** Public music library, through MediaStore on API 29+ and a plain directory below that. */
    object MusicLibrary : ConversionDestination

    /** Folder the user picked through the system picker (SAF). */
    data class Folder(val treeUri: Uri) : ConversionDestination
}
