package com.example.hapticogg.ui

import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

/**
 * "1 · 选择音频" card: the system picker gives no way to cancel without walking back out of the
 * folder tree, so the selection itself is editable here instead - one row per file with a
 * "移除" action, plus "清空选择" for all of them. Picking again adds to the list.
 */
class SelectionSection(private val activity: ComponentActivity) {

    /** Opens the system picker; the picked files are added to the current selection. */
    var onRequestPick: () -> Unit = {}

    /** Drops one file; the index refers to the list last handed to [show]. */
    var onRequestRemove: (Int) -> Unit = {}

    var onRequestClear: () -> Unit = {}

    /** The whole card, ready to be added to the screen. */
    val view: LinearLayout

    private val summary: TextView
    private val rows: LinearLayout
    private val clearButton: Button

    init {
        rows = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        summary = Fluent.caption(activity, "")
        clearButton = Fluent.secondaryButton(activity, "清空选择") { onRequestClear() }
        view = Fluent.card(activity).apply {
            addView(Fluent.section(activity, "1 · 选择音频"))
            addView(
                Fluent.primaryButton(activity, "选择音频（可多选）") { onRequestPick() },
                Fluent.rowParams(activity, 10)
            )
            addView(summary, Fluent.rowParams(activity, 10))
            addView(rows, Fluent.rowParams(activity, 2))
            addView(clearButton, Fluent.rowParams(activity, 10))
        }
        // First launch state: nothing picked, so the clear action stays hidden.
        show(emptyList())
    }

    /** Shows the picked files in selection order. */
    fun show(names: List<String>) {
        rows.removeAllViews()
        summary.text = if (names.isEmpty()) {
            "尚未选择文件"
        } else {
            "已选择 ${names.size} 个文件（再次选择会追加）"
        }
        clearButton.visibility = if (names.isEmpty()) View.GONE else View.VISIBLE
        names.forEachIndexed { index, name -> rows.addView(row(name, index)) }
    }

    private fun row(name: String, index: Int): View = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(
            Fluent.body(activity, name).apply {
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.MIDDLE
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )
        addView(
            Fluent.linkButton(activity, "移除") { onRequestRemove(index) },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = Fluent.dp(activity, 8) }
        )
    }
}
