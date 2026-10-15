package com.example.hapticogg.conversion

import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.example.hapticogg.data.ContentUriFiles
import com.example.hapticogg.data.ConvertedFileStore
import com.example.hapticogg.export.MediaExporter
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** The files to convert and where the results go. */
data class ConversionRequest(val uris: List<Uri>, val destination: ConversionDestination)

/** Outcome of a finished batch. */
data class ConversionReport(
    /** How many files were asked for, including the ones that failed. */
    val requested: Int,
    /** Display names of the files that were converted and exported. */
    val exported: List<String>,
    /** One message per file that could not be converted. */
    val errors: List<String>,
    /** Local copy of the last successfully converted file, if any. */
    val lastOutput: File?
)

/**
 * Converts picked audio files one after another on a background thread. Progress and the
 * final report are delivered on the main thread.
 */
class BatchConverter(
    private val exporter: MediaExporter,
    private val files: ConvertedFileStore,
    private val sources: ContentUriFiles
) {
    private val executor = Executors.newSingleThreadExecutor()
    private val running = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Progress while a batch runs. */
    interface Callbacks {
        fun onProgress(index: Int, total: Int, sourceName: String)
        fun onFinished(report: ConversionReport)
    }

    fun isRunning(): Boolean = running.get()

    /** Starts a batch. @return false when one is already running. */
    fun convert(request: ConversionRequest, callbacks: Callbacks): Boolean {
        if (!running.compareAndSet(false, true)) return false
        executor.execute { run(request, callbacks) }
        return true
    }

    /** Stops accepting new batches; a running one still finishes. */
    fun shutdown() {
        executor.shutdown()
    }

    private fun run(request: ConversionRequest, callbacks: Callbacks) {
        val exported = mutableListOf<String>()
        val errors = mutableListOf<String>()
        var lastOutput: File? = null

        request.uris.forEachIndexed { index, uri ->
            val sourceName = sources.displayName(uri)
            post { callbacks.onProgress(index, request.uris.size, sourceName) }
            var input: File? = null
            try {
                input = sources.copyToCache(uri, sourceName)
                val exportName = ExportNames.convertedName(sourceName)
                val output = files.outputFile(exportName)
                val session = FFmpegKit.execute(
                    ConversionCommand.build(input, output, ExportNames.titleOf(sourceName))
                )
                if (!ReturnCode.isSuccess(session.returnCode) || !output.exists() || output.length() == 0L) {
                    errors.add("$sourceName：${session.failStackTrace ?: session.allLogsAsString.takeLast(600)}")
                    output.delete()
                } else {
                    when (val destination = request.destination) {
                        ConversionDestination.MusicLibrary ->
                            exporter.toMusicLibrary(output, exportName)
                        is ConversionDestination.Folder ->
                            exporter.toFolder(output, destination.treeUri, exportName)
                    }
                    exported.add(exportName)
                    lastOutput = output
                }
            } catch (e: Exception) {
                errors.add("$sourceName：${e.message}")
            } finally {
                input?.delete()
            }
        }

        post {
            running.set(false)
            callbacks.onFinished(
                ConversionReport(request.uris.size, exported.toList(), errors.toList(), lastOutput)
            )
        }
    }

    private fun post(block: () -> Unit) {
        mainHandler.post(block)
    }
}
