package com.example.hapticogg.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

/**
 * A small Fluent 2 inspired toolkit: acrylic surfaces with rounded corners, depth,
 * a restrained accent colour and the Fluent type ramp. Android has no official
 * Fluent UI library, so the visual language is reproduced with platform views.
 */
object Fluent {
    const val ACCENT = 0xFF0F6CBD.toInt()
    const val ACCENT_PRESSED = 0xFF115EA3.toInt()
    const val SURFACE = 0xFFFFFFFF.toInt()
    const val STROKE = 0xFFE0E0E0.toInt()
    const val TEXT = 0xFF242424.toInt()
    const val TEXT_SECONDARY = 0xFF616161.toInt()
    const val DANGER = 0xFFC4314B.toInt()
    private const val RIPPLE = 0x26000000

    fun dp(context: Context, value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()

    /** Transparent system bars with dark icons, matching the light Fluent canvas. */
    fun enableEdgeToEdge(activity: ComponentActivity) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
    }

    /** Acrylic style surface: soft rounded panel with a hairline stroke. */
    fun acrylic(context: Context, radiusDp: Int = 8, fill: Int = SURFACE, stroke: Int = STROKE): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, radiusDp).toFloat()
            setColor(fill)
            setStroke(dp(context, 1), stroke)
        }

    fun card(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = acrylic(context, 8)
        elevation = dp(context, 2).toFloat()
        val pad = dp(context, 16)
        setPadding(pad, pad, pad, pad)
    }

    /** Primary command: accent fill, white label. */
    fun primaryButton(context: Context, label: String, onClick: (() -> Unit)? = null): Button = Button(context).apply {
        text = label
        isAllCaps = false
        textSize = 14f
        setTextColor(Color.WHITE)
        stateListAnimator = null
        background = RippleDrawable(
            ColorStateList.valueOf(RIPPLE),
            acrylic(context, 6, ACCENT, ACCENT_PRESSED),
            null
        )
        minHeight = dp(context, 44)
        elevation = dp(context, 1).toFloat()
        if (onClick != null) setOnClickListener { onClick() }
    }

    /** Secondary command: neutral surface with a hairline stroke. */
    fun secondaryButton(context: Context, label: String, onClick: (() -> Unit)? = null): Button = Button(context).apply {
        text = label
        isAllCaps = false
        textSize = 14f
        setTextColor(TEXT)
        stateListAnimator = null
        background = RippleDrawable(
            ColorStateList.valueOf(RIPPLE),
            acrylic(context, 6, SURFACE, STROKE),
            null
        )
        minHeight = dp(context, 44)
        if (onClick != null) setOnClickListener { onClick() }
    }

    fun dangerButton(context: Context, label: String, onClick: (() -> Unit)? = null): Button = Button(context).apply {
        text = label
        isAllCaps = false
        textSize = 14f
        setTextColor(Color.WHITE)
        stateListAnimator = null
        background = RippleDrawable(
            ColorStateList.valueOf(RIPPLE),
            acrylic(context, 6, DANGER, DANGER),
            null
        )
        minHeight = dp(context, 44)
        if (onClick != null) setOnClickListener { onClick() }
    }

    fun title(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        textSize = 26f
        setTextColor(TEXT)
        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
    }

    fun subtitle(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        textSize = 13f
        setTextColor(TEXT_SECONDARY)
    }

    /** Fluent section header: medium weight, tight leading. */
    fun section(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        textSize = 14f
        setTextColor(TEXT)
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        letterSpacing = 0.01f
    }

    fun body(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        textSize = 13f
        setTextColor(TEXT)
    }

    fun caption(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        textSize = 11.5f
        setTextColor(TEXT_SECONDARY)
    }

    /** Compact text command for row level actions, such as removing one picked file. */
    fun linkButton(context: Context, label: String, onClick: () -> Unit): TextView = TextView(context).apply {
        text = label
        textSize = 13f
        setTextColor(ACCENT)
        isClickable = true
        val padH = dp(context, 10)
        val padV = dp(context, 8)
        setPadding(padH, padV, padH, padV)
        background = RippleDrawable(ColorStateList.valueOf(RIPPLE), null, null)
        setOnClickListener { onClick() }
    }

    fun hairline(context: Context): View = View(context).apply {
        setBackgroundColor(STROKE)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 1))
    }

    fun rowParams(context: Context, topDp: Int = 8, bottomDp: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(context, topDp)
            bottomMargin = dp(context, bottomDp)
        }

}
