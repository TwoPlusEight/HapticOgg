package com.example.hapticogg.conversion

import java.io.File

/** Builds the FFmpeg command for the haptic OGG. */
object ConversionCommand {

    /**
     * Three channel Vorbis OGG: the original stereo plus a low frequency haptic track,
     * tagged with android_haptic=1.
     */
    fun build(input: File, output: File, title: String): String {
        output.delete()
        val filter = "[0:a]aresample=48000,aformat=channel_layouts=stereo,asplit=2[st][src];" +
            "[src]pan=mono|c0=0.5*c0+0.5*c1," +
            "highpass=f=$DEFAULT_LOW_HZ,lowpass=f=$DEFAULT_HIGH_HZ,volume=$DEFAULT_STRENGTH[h];" +
            "[st][h]amerge=inputs=2[out]"
        return listOf(
            "-hide_banner -y -i ${quote(input.absolutePath)}",
            "-filter_complex ${quote(filter)}",
            "-map ${quote("[out]")}",
            "-c:a libvorbis -q:a 5",
            "-metadata ${quote("android_haptic=1")}",
            "-metadata ${quote("title=$title")}",
            quote(output.absolutePath)
        ).joinToString(" ")
    }

    private fun quote(value: String) = "'" + value.replace("'", "'\\''") + "'"

    /** Low frequency band used for the haptic track. */
    const val DEFAULT_LOW_HZ = 45
    const val DEFAULT_HIGH_HZ = 180
    /** Relative level of the haptic track. */
    const val DEFAULT_STRENGTH = 0.65
}
