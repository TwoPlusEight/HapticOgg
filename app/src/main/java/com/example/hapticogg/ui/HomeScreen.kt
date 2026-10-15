package com.example.hapticogg.ui

import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.io.File

/**
 * View hierarchy of the home screen: it builds the widgets and shows what it is told.
 * Everything the user does is reported through the callbacks, so the activity keeps the
 * state and this class stays free of app logic.
 */
class HomeScreen(private val activity: ComponentActivity) {

    /** Tapped "选择音频（可多选）". */
    var onPickAudio: () -> Unit = {}
    /** Tapped "移除" next to one picked file; the index is its position in the shown list. */
    var onRemoveSelectedFile: (Int) -> Unit = {}
    /** Tapped "清空选择". */
    var onClearSelection: () -> Unit = {}
    /** Tapped "修改导出文件位置". */
    var onChooseDestination: () -> Unit = {}
    /** Tapped "开始转换". */
    var onConvert: () -> Unit = {}
    /** Tapped "测试振动（播放）". */
    var onTestVibration: () -> Unit = {}
    /** Tapped a row of the converted file list. */
    var onOpenConvertedFile: (File) -> Unit = {}
    /** Swiped a row of the converted file list far enough to delete it. */
    var onDeleteConvertedFile: (File) -> Unit = {}

    private val page = FluentPage(activity)

    /** Root view, handed to `setContentView`. */
    val root: View get() = page.root

    private val selectionSection: SelectionSection
    private lateinit var destination: TextView
    private lateinit var status: TextView
    private lateinit var convertButton: Button
    private val convertedSection: ConvertedFilesSection

    init {
        val column = page.column
        selectionSection = SelectionSection(activity).apply {
            onRequestPick = { onPickAudio() }
            onRequestRemove = { onRemoveSelectedFile(it) }
            onRequestClear = { onClearSelection() }
        }
        convertedSection = ConvertedFilesSection(activity).apply {
            onOpenFile = { onOpenConvertedFile(it) }
            onDeleteRequested = { onDeleteConvertedFile(it) }
        }

        column.addView(header())
        column.addView(selectionSection.view, Fluent.rowParams(activity, 16))
        column.addView(destinationCard(), Fluent.rowParams(activity, 16))
        column.addView(actionCard(), Fluent.rowParams(activity, 12))
        column.addView(convertedSection.view, Fluent.rowParams(activity, 12))

        // After the first "show more" tap the rest of the list loads as the user scrolls.
        page.root.setOnScrollChangeListener { _, _, _, _, _ -> loadMoreIfNearBottom() }
    }

    /** Text of the media processing status line. */
    fun showStatus(text: String) {
        status.text = text
    }

    /** Names of the picked files, in selection order. */
    fun showSelection(names: List<String>) {
        selectionSection.show(names)
    }

    /** Where the converted files go. */
    fun showDestination(text: String) {
        destination.text = text
    }

    /** Enables or disables the convert command while a batch runs. */
    fun setConvertEnabled(enabled: Boolean) {
        convertButton.isEnabled = enabled
    }

    /** Fills the converted file list and marks the files that still have an exported copy. */
    fun showConvertedFiles(files: List<File>, exportedNames: Set<String>) {
        convertedSection.show(files, exportedNames)
    }

    private fun header(): View = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        addView(Fluent.title(activity, "Haptic OGG"))
        addView(
            Fluent.subtitle(activity, "将无同步震动的音频文件添加一条轨道来实现同步震动\n选择导出的音频后在系统内可以选择振动与铃声同步"),
            Fluent.rowParams(activity, 4)
        )
    }

    private fun destinationCard(): View = Fluent.card(activity).apply {
        addView(Fluent.section(activity, "2 · 导出位置"))
        destination = Fluent.body(activity, "")
        addView(destination, Fluent.rowParams(activity, 8))
        addView(
            Fluent.secondaryButton(activity, "修改导出文件位置") { onChooseDestination() },
            Fluent.rowParams(activity, 10)
        )
    }

    private fun actionCard(): View = Fluent.card(activity).apply {
        addView(Fluent.section(activity, "3 · 转换与测试"))
        convertButton = Fluent.primaryButton(activity, "开始转换") { onConvert() }
        addView(convertButton, Fluent.rowParams(activity, 10))
        addView(
            Fluent.secondaryButton(activity, "测试振动（播放）") { onTestVibration() },
            Fluent.rowParams(activity, 8)
        )
        addView(
            Fluent.caption(
                activity,
                "播放测试会同步振动，在播放界面里可以直接把该文件设为铃声。\n如果振动意外消失暂停重新播放即可\n此应用内播放的振动强度可能会比系统内弱.\n\n# 以下是媒体处理状态:"
            ),
            Fluent.rowParams(activity, 10)
        )
        status = Fluent.caption(activity, "就绪")
        addView(status, Fluent.rowParams(activity, 12))
    }

    private fun loadMoreIfNearBottom() {
        val content = page.root.getChildAt(0) ?: return
        if (content.height <= 0) return
        if (page.root.scrollY + page.root.height >= content.height - dp(240)) {
            convertedSection.loadMoreOnScroll()
        }
    }

    private fun dp(value: Int): Int = Fluent.dp(activity, value)
}
