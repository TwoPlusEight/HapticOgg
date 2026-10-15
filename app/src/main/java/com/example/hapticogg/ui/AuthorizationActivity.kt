package com.example.hapticogg.ui

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import com.example.hapticogg.export.ExportSpec

/**
 * First launch consent screen: may the app save every converted file into the system music
 * directory? It asks the question the home screen used to show as a dialog, but as a page,
 * so the explanation gets the room it needs.
 *
 * The answer is reported to the caller as [RESULT_AGREED] or [RESULT_DECLINED]; leaving the
 * screen with the back gesture reports [Activity.RESULT_CANCELED], which the caller treats
 * the same way as a refusal.
 */
class AuthorizationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fluent.enableEdgeToEdge(this)

        val page = FluentPage(this)
        val column = page.column
        column.addView(Fluent.title(this, "Haptic OGG 导出音频授权"))

        val card = Fluent.card(this)
        card.addView(
            Fluent.body(this, "本应用不进行任何联网，全程本地处理，无需担心隐私泄露问题，但需要您授权或者自行修改保存位置。"),
            Fluent.rowParams(this, 10)
        )
        card.addView(
            Fluent.body(this, "是否允许本应用把每次转换生成的 OGG 自动保存到系统音乐目录？"),
            Fluent.rowParams(this, 16)
        )
        card.addView(Fluent.body(this, agreeText()), Fluent.rowParams(this, 12))
        card.addView(Fluent.body(this, declineText()), Fluent.rowParams(this, 8))
        card.addView(answerRow(), Fluent.rowParams(this, 20))
        column.addView(card, Fluent.rowParams(this, 16))

        setContentView(page.root)
    }

    private fun answerRow(): LinearLayout {
        val gap = Fluent.dp(this, 8)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val agree = Fluent.primaryButton(this, "同意") { finishWith(RESULT_AGREED) }
        val decline = Fluent.secondaryButton(this, "不同意") { finishWith(RESULT_DECLINED) }
        row.addView(agree, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(
            decline,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = gap
            }
        )
        return row
    }

    /** Android 9 and older need the storage permission before the first write. */
    private fun agreeText(): String = buildString {
        append("· 同意：保存到「音乐/${ExportSpec.MUSIC_DIR}」，转换完不用再手动导出。")
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) append(" 系统会先弹出存储权限请求。")
    }

    private fun declineText(): String =
        "· 不同意：随时可用「修改导出文件位置」选择自己想要的文件夹。"

    private fun finishWith(resultCode: Int) {
        setResult(resultCode)
        finish()
    }

    companion object {
        /** Result code: the user lets the app save into the system music directory. */
        const val RESULT_AGREED = Activity.RESULT_OK

        /** Result code: the user would rather pick a folder himself. */
        const val RESULT_DECLINED = Activity.RESULT_FIRST_USER
    }
}
