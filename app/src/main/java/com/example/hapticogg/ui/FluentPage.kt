package com.example.hapticogg.ui

import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * A scrollable Fluent page: one vertical column inside a scroll view, padded so the content
 * never slides under the status bar, navigation bar, display cutout or the keyboard.
 */
internal class FluentPage(activity: ComponentActivity) {

    private val horizontalPadding = Fluent.dp(activity, 16)
    private val verticalPadding = Fluent.dp(activity, 16)

    /** Add the cards and other rows here. */
    val column: LinearLayout = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }

    /** Page root, handed to `setContentView`. */
    val root: ScrollView = ScrollView(activity).apply {
        addView(column)
        // clipToPadding stays enabled: scrolled content must never slide under the transparent
        // status bar, otherwise it overlaps the clock and the system icons.
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
    }

    init {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(
                horizontalPadding + bars.left,
                verticalPadding + bars.top,
                horizontalPadding + bars.right,
                verticalPadding + maxOf(bars.bottom, ime.bottom)
            )
            insets
        }
    }
}
