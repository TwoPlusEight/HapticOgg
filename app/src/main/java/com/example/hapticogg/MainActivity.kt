package com.example.hapticogg

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.hapticogg.conversion.BatchConverter
import com.example.hapticogg.conversion.ConversionDestination
import com.example.hapticogg.conversion.ConversionReport
import com.example.hapticogg.conversion.ConversionRequest
import com.example.hapticogg.data.ContentUriFiles
import com.example.hapticogg.data.ConvertedFileStore
import com.example.hapticogg.data.DestinationMode
import com.example.hapticogg.data.DestinationPreferences
import com.example.hapticogg.export.ExportSpec
import com.example.hapticogg.export.ExportedCopies
import com.example.hapticogg.export.MediaExporter
import com.example.hapticogg.export.RingtoneInstaller
import com.example.hapticogg.ui.AuthorizationActivity
import com.example.hapticogg.ui.Fluent
import com.example.hapticogg.ui.HapticTestDialog
import com.example.hapticogg.ui.HomeScreen
import java.io.File

/** A file the user picked, together with the display name the provider reported for it. */
private data class PickedAudio(val uri: Uri, val name: String)

/**
 * Home screen of the app.
 *
 * The layout lives in [HomeScreen], the actual work in the `conversion`, `export` and
 * `data` packages. This activity owns the Android plumbing (activity results, runtime
 * permissions, dialogs) and wires those pieces together.
 */
class MainActivity : ComponentActivity() {

    private val prefs by lazy { DestinationPreferences(this) }
    private val convertedFiles by lazy { ConvertedFileStore(this) }
    private val sources by lazy { ContentUriFiles(this) }
    private val exporter by lazy { MediaExporter(this) }
    private val exportedCopies by lazy { ExportedCopies(this) }
    private val ringtoneInstaller by lazy { RingtoneInstaller(this) }
    private val converter by lazy { BatchConverter(exporter, convertedFiles, sources) }

    private val selected = mutableListOf<PickedAudio>()
    private lateinit var screen: HomeScreen

    /** Set while a folder pick should start the conversion as soon as it returns. */
    private var convertAfterFolderPick = false

    /** Destination of the running batch, kept for the completion message. */
    private var activeDestinationLabel: String? = null

    private val pickAudio =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNullOrEmpty()) {
                // Leaving the picker keeps whatever is selected already.
                toast("未添加文件，当前选择未改变。")
                return@registerForActivityResult
            }
            val added = uris.count { addToSelection(it) }
            refreshSelection()
            toast(if (added == 0) "这些文件已经在选择里了。" else "已添加 $added 个文件")
        }

    private val pickFolder =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) {
                // Leaving the picker without choosing keeps the previous destination.
                val wasConverting = convertAfterFolderPick
                convertAfterFolderPick = false
                screen.showStatus(
                    if (wasConverting) "未选择文件夹，转换已取消。"
                    else "未选择文件夹，导出位置未改变。"
                )
                return@registerForActivityResult
            }
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers only hand out session scoped access.
            }
            prefs.treeUri = uri
            setMode(DestinationMode.FOLDER)
            screen.showStatus("导出位置已更新：${exporter.describeTree(uri)}")
            if (convertAfterFolderPick) {
                convertAfterFolderPick = false
                startConversion()
            }
        }

    private val requestStorage =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                setMode(DestinationMode.MUSIC)
                screen.showStatus("已获得存储权限，转换结果会保存到 音乐/${ExportSpec.MUSIC_DIR}。")
            } else {
                setMode(DestinationMode.FOLDER)
                screen.showStatus("未获得存储权限。请用「修改导出文件位置」选择导出文件夹。")
            }
        }

    /** First launch consent, answered on [AuthorizationActivity]. */
    private val askForAuthorization =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AuthorizationActivity.RESULT_AGREED) {
                onAuthorizationAgreed()
            } else {
                // RESULT_DECLINED, or the user left the screen with the back gesture.
                onAuthorizationDeclined()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fluent.enableEdgeToEdge(this)
        screen = HomeScreen(this).apply {
            onPickAudio = { pickAudio.launch(arrayOf("audio/flac", "audio/*")) }
            onRemoveSelectedFile = { removeSelectedFile(it) }
            onClearSelection = { clearSelection() }
            onChooseDestination = { chooseDestination() }
            onConvert = { startConversion() }
            onTestVibration = { openTestDialog() }
            onOpenConvertedFile = { openTestDialog(it) }
            onDeleteConvertedFile = { confirmDelete(it) }
        }
        setContentView(screen.root)
        refreshSelection()
        refreshConvertedFiles()
        refreshDestination()
        // The answer is re-delivered after a config change, so only ask on a fresh start.
        if (savedInstanceState == null) askForAuthorizationIfNeeded()
    }

    // ------------------------------------------------------------------ selection

    /** @return true when the file was not part of the selection yet. */
    private fun addToSelection(uri: Uri): Boolean {
        if (selected.any { it.uri == uri }) return false
        selected += PickedAudio(uri, sources.displayName(uri))
        return true
    }

    private fun refreshSelection() {
        if (!::screen.isInitialized) return
        screen.showSelection(selected.map { it.name })
    }

    /**
     * Drops one picked file. The system picker has no cancel button, so a wrong selection is
     * fixed here instead of by walking back out of the picker's folder tree.
     */
    private fun removeSelectedFile(index: Int) {
        if (index !in selected.indices) return
        val removed = selected.removeAt(index)
        refreshSelection()
        toast("已移除 ${removed.name}")
    }

    private fun clearSelection() {
        if (selected.isEmpty()) return
        selected.clear()
        refreshSelection()
        toast("已清空选择")
    }

    // ------------------------------------------------------------- converted files

    private fun refreshConvertedFiles() {
        val converted = convertedFiles.listConverted()
        dropStaleRecord(converted)
        // Records come from the local folder; this compares them with what was actually exported.
        val exportedNames = exportedCopies.ownNames() + exportedCopies.folderNames(prefs.treeUri)
        screen.showConvertedFiles(converted, exportedNames)
    }

    /** Keeps the app's stored "last conversion" record in sync with the files on disk. */
    private fun dropStaleRecord(converted: List<File>) {
        val file = prefs.lastOutput ?: return
        if (converted.any { it.absolutePath == file.absolutePath }) return
        prefs.lastOutput = null
    }

    private fun confirmDelete(file: File) {
        val copies = exportedCopies.find(file.name, prefs.treeUri)
        AlertDialog.Builder(this)
            .setTitle("删除这条记录？")
            .setMessage(
                buildString {
                    append("本地副本：${file.name}")
                    append("\n本地包络：${file.nameWithoutExtension}.env")
                    append(
                        if (copies.isEmpty()) "\n没有找到导出副本（可能已被手动删除）"
                        else "\n导出副本：${copies.size} 个（音乐/Ringtones/所选文件夹中的同名文件）"
                    )
                    append("\n\n删除后不可恢复。")
                }
            )
            .setPositiveButton("删除") { _, _ -> deleteRecord(file) }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteRecord(file: File) {
        val removedLocal = convertedFiles.delete(file)
        val removedExported = exportedCopies.delete(exportedCopies.find(file.name, prefs.treeUri))
        if (prefs.lastOutput?.absolutePath == file.absolutePath) prefs.lastOutput = null
        refreshConvertedFiles()
        screen.showStatus(
            "已删除 ${file.name}：本地 $removedLocal 个文件" +
                (if (removedExported > 0) "，导出副本 $removedExported 个" else "，未找到导出副本")
        )
    }

    // ------------------------------------------------------------------ destination

    private fun setMode(mode: DestinationMode) {
        prefs.mode = mode
        refreshDestination()
    }

    private fun refreshDestination() {
        if (!::screen.isInitialized) return
        val tree = prefs.treeUri
        screen.showDestination(
            when {
                prefs.mode == DestinationMode.MUSIC ->
                    "导出位置：音乐/${ExportSpec.MUSIC_DIR}（转换完成后自动保存）"
                tree != null -> "导出位置：${exporter.describeTree(tree)}"
                else -> "导出位置：尚未选择（点「开始转换」时会先让你挑文件夹）"
            }
        )
    }

    /**
     * The first launch consent lives in [AuthorizationActivity] because a full screen suits
     * the explanation better than a dialog. It is asked again until it has been answered, or
     * while the legacy storage permission the music directory needs is missing.
     */
    private fun askForAuthorizationIfNeeded() {
        val permissionMissing = prefs.mode == DestinationMode.MUSIC && needsLegacyStoragePermission()
        if (prefs.consentAnswered && !permissionMissing) return
        askForAuthorization.launch(Intent(this, AuthorizationActivity::class.java))
    }

    private fun onAuthorizationAgreed() {
        prefs.consentAnswered = true
        if (needsLegacyStoragePermission()) {
            requestStorage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            setMode(DestinationMode.MUSIC)
            screen.showStatus("转换结果将自动保存到 音乐/${ExportSpec.MUSIC_DIR}。")
        }
    }

    private fun onAuthorizationDeclined() {
        prefs.consentAnswered = true
        setMode(DestinationMode.FOLDER)
        screen.showStatus("请用「修改导出文件位置」选择导出文件夹，或点「开始转换」时再选。")
    }

    private fun chooseDestination() {
        AlertDialog.Builder(this)
            .setTitle("导出文件位置")
            .setItems(
                arrayOf(
                    "系统音乐目录（音乐/${ExportSpec.MUSIC_DIR}）",
                    "选择其它文件夹…"
                )
            ) { _, which ->
                if (which == 0) {
                    if (needsLegacyStoragePermission()) {
                        requestStorage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else {
                        setMode(DestinationMode.MUSIC)
                        screen.showStatus("导出位置：音乐/${ExportSpec.MUSIC_DIR}")
                    }
                } else {
                    pickFolder.launch(prefs.treeUri)
                }
            }
            .show()
    }

    /** API 28 and older need the storage permission to write into the public music directory. */
    private fun needsLegacyStoragePermission(): Boolean =
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED

    // ------------------------------------------------------------------- conversion

    private fun startConversion() {
        if (converter.isRunning()) return
        if (selected.isEmpty()) {
            toast("请先选择一个或多个音频文件")
            return
        }
        val destination = currentDestination()
        if (destination == null) {
            askForFolderBeforeConverting()
            return
        }
        activeDestinationLabel = destinationLabel(destination)
        val request = ConversionRequest(selected.map { it.uri }, destination)
        if (!converter.convert(request, conversionCallbacks)) return
        screen.setConvertEnabled(false)
    }

    private fun askForFolderBeforeConverting() {
        AlertDialog.Builder(this)
            .setTitle("还没有选择导出文件夹")
            .setMessage("转换结果需要一个保存位置。现在选择文件夹，或者改用系统音乐目录？")
            .setPositiveButton("选择文件夹") { _, _ ->
                convertAfterFolderPick = true
                screen.showStatus("请选择导出文件夹…")
                pickFolder.launch(null)
            }
            .setNeutralButton("用音乐目录") { _, _ ->
                setMode(DestinationMode.MUSIC)
                startConversion()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** Where the next batch goes, or null when the folder destination is still missing. */
    private fun currentDestination(): ConversionDestination? = when (prefs.mode) {
        DestinationMode.MUSIC -> ConversionDestination.MusicLibrary
        DestinationMode.FOLDER -> prefs.treeUri?.let { ConversionDestination.Folder(it) }
    }

    private fun destinationLabel(destination: ConversionDestination): String = when (destination) {
        ConversionDestination.MusicLibrary -> "音乐/${ExportSpec.MUSIC_DIR}"
        is ConversionDestination.Folder -> exporter.describeTree(destination.treeUri)
    }

    private val conversionCallbacks = object : BatchConverter.Callbacks {
        override fun onProgress(index: Int, total: Int, sourceName: String) = ifAlive {
            screen.showStatus("正在转换 ${index + 1}/$total：$sourceName")
        }

        override fun onFinished(report: ConversionReport) = ifAlive {
            report.lastOutput?.let { prefs.lastOutput = it }
            screen.setConvertEnabled(true)
            refreshConvertedFiles()
            screen.showStatus(
                buildString {
                    append("转换完成：成功 ${report.exported.size}/${report.requested} 个。")
                    report.lastOutput?.let { append("\n最近文件：${it.name}") }
                    append("\n导出位置：$activeDestinationLabel")
                    if (report.exported.isNotEmpty()) {
                        append("\n可在「已转换文件」里点条目播放或设为铃声。")
                    }
                    if (report.errors.isNotEmpty()) {
                        append("\n失败详情：\n" + report.errors.joinToString("\n"))
                    }
                }
            )
        }
    }

    // ----------------------------------------------------------------------- player

    private fun openTestDialog() {
        val target = prefs.lastOutput?.takeIf { it.exists() }
        if (target == null) {
            screen.showStatus("还没有转换结果，先转换一个文件。")
            return
        }
        openTestDialog(target)
    }

    /** The local copy is all the player needs, so opening a row cannot fail on the export. */
    private fun openTestDialog(file: File) {
        HapticTestDialog(this, file) { ringtoneInstaller.install(it) }.show()
    }

    // ---------------------------------------------------------------------- helpers

    /** Batch callbacks already arrive on the main thread; skip them once the screen is gone. */
    private fun ifAlive(block: () -> Unit) {
        if (!isFinishing && !isDestroyed) block()
    }

    /**
     * Short feedback for the user's own actions. Selection changes and input validation report
     * this way; the status line under the convert card stays for conversion progress and results.
     */
    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        converter.shutdown()
        super.onDestroy()
    }
}
