package com.example.hapticogg.ui

import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * "已转换文件" card: newest file first, paged, one row per converted file. Tapping a row
 * opens the player, swiping it left asks to delete it; both are reported to the owner.
 */
class ConvertedFilesSection(private val activity: ComponentActivity) {

    var onOpenFile: (File) -> Unit = {}
    var onDeleteRequested: (File) -> Unit = {}

    /** The whole card, ready to be added to the screen. */
    val view: LinearLayout

    private val header: TextView
    private val rows: LinearLayout
    private val moreButton: Button

    private var files: List<File> = emptyList()
    private var exportedNames: Set<String> = emptySet()
    private var visibleCount = DEFAULT_VISIBLE_FILES
    private var expanded = false

    init {
        rows = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        header = Fluent.caption(activity, "")
        moreButton = Fluent.secondaryButton(activity, "显示更多") { showMore() }
        view = Fluent.card(activity).apply {
            addView(Fluent.section(activity, "已转换文件"))
            addView(header, Fluent.rowParams(activity, 8))
            addView(rows, Fluent.rowParams(activity, 4))
            addView(moreButton, Fluent.rowParams(activity, 8))
        }
    }

    /** Shows the converted files, newest first, and which of them still have an export. */
    fun show(files: List<File>, exportedNames: Set<String>) {
        this.files = files
        this.exportedNames = exportedNames
        render()
    }

    /** Tapped "显示更多"; afterwards scrolling keeps loading the remaining rows. */
    fun showMore() {
        expanded = true
        loadNextPage()
    }

    /** Called while scrolling: pages through the list once more rows were asked for. */
    fun loadMoreOnScroll() {
        if (expanded) loadNextPage()
    }

    private fun loadNextPage() {
        if (visibleCount >= files.size) return
        visibleCount = (visibleCount + PAGE_SIZE).coerceAtMost(files.size)
        render()
    }

    private fun render() {
        rows.removeAllViews()
        val shown = files.take(visibleCount)
        header.text = if (files.isEmpty()) {
            "还没有转换结果"
        } else {
            "共 ${files.size} 个，已显示 ${shown.size} 个；点击条目可播放 / 测试振动"
        }
        shown.forEachIndexed { index, file ->
            if (index > 0) rows.addView(separator())
            rows.addView(row(file))
        }
        moreButton.visibility = if (shown.size < files.size) View.VISIBLE else View.GONE
    }

    private fun row(file: File): View {
        val deleteLabel = TextView(activity).apply {
            text = "删除"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            setPadding(0, 0, Fluent.dp(activity, 24), 0)
            setBackgroundColor(Fluent.DANGER)
        }
        val container = FrameLayout(activity)
        container.addView(
            deleteLabel,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )

        val pad = Fluent.dp(activity, 12)
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(Fluent.SURFACE)
            isClickable = true
            setOnClickListener { onOpenFile(file) }
        }
        content.addView(Fluent.body(activity, file.name))
        content.addView(
            Fluent.caption(activity, "").apply { text = detailsOf(file) },
            Fluent.rowParams(activity, 2)
        )
        container.addView(
            content,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        )
        content.setOnTouchListener(
            SwipeToDeleteListener(activity, content) { onDeleteRequested(file) }
        )
        return container
    }

    private fun detailsOf(file: File): String = listOf(
        formatSize(file.length()),
        ROW_TIME.format(Instant.ofEpochMilli(file.lastModified()).atZone(ZoneId.systemDefault())),
        if (file.name in exportedNames) "导出副本在" else "导出副本已不存在"
    ).joinToString(" · ")

    private fun separator() = View(activity).apply {
        setBackgroundColor(Fluent.STROKE)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, Fluent.dp(activity, 1)
        )
    }

    private fun formatSize(bytes: Long): String = when {
        bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
        bytes >= 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }

    private companion object {
        const val DEFAULT_VISIBLE_FILES = 5
        const val PAGE_SIZE = 5
        val ROW_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.US)
    }
}
