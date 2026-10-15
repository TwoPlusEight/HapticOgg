package com.example.hapticogg.conversion

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** File names of the converted OGGs. */
object ExportNames {

    /** Name for the OGG converted from [sourceName]: sanitised base plus a timestamp. */
    fun convertedName(sourceName: String): String =
        sanitisedBase(sourceName) + "_" + STAMP.format(LocalDateTime.now()) + ".ogg"

    /** Title tag of the converted file: the original name without its extension. */
    fun titleOf(sourceName: String): String = sourceName.substringBeforeLast('.', sourceName)

    private fun sanitisedBase(sourceName: String): String =
        sourceName.substringBeforeLast('.', sourceName)
            .replace(ILLEGAL_CHARS, "_")
            .trim()
            .take(MAX_BASE_LENGTH)
            .ifBlank { DEFAULT_BASE }

    private const val MAX_BASE_LENGTH = 80
    private const val DEFAULT_BASE = "audio"
    private val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    private val ILLEGAL_CHARS = Regex("[\\\\/:*?\"<>|\\u0000-\\u001f]")
}
