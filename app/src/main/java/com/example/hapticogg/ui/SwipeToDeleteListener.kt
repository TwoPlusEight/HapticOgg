package com.example.hapticogg.ui

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs

/**
 * Left swipe on a list row: the row follows the finger, and a swipe past [CONFIRM_RATIO]
 * of the row width asks for confirmation when the finger lifts.
 */
class SwipeToDeleteListener(
    context: Context,
    private val content: View,
    private val onConfirm: () -> Unit
) : View.OnTouchListener {

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var dragging = false
    private var moved = false

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                dragging = false
                moved = false
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                if (!dragging && dx < -slop && abs(dx) > abs(dy) * 1.5f) {
                    dragging = true
                    content.isPressed = false
                    content.parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (dragging) {
                    content.translationX = dx.coerceIn(-view.width.toFloat(), 0f)
                    return true
                }
                if (!moved && (abs(dx) > slop || abs(dy) > slop)) {
                    // Dragged without a horizontal lock (e.g. the list cannot scroll any
                    // further): swallow the gesture so it is never reported as a tap.
                    moved = true
                    content.isPressed = false
                }
                if (moved) return true
                return false
            }

            MotionEvent.ACTION_UP -> {
                if (!dragging) return moved
                content.parent?.requestDisallowInterceptTouchEvent(false)
                val swipedFar = content.translationX < -view.width * CONFIRM_RATIO
                content.animate().translationX(0f).setDuration(150).start()
                dragging = false
                if (swipedFar) onConfirm()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                // The list took over the gesture (scrolling) - never treat this as a tap.
                if (!dragging) return moved
                content.parent?.requestDisallowInterceptTouchEvent(false)
                content.animate().translationX(0f).setDuration(150).start()
                dragging = false
                return true
            }
        }
        return dragging
    }

    private companion object {
        /** Fraction of the row width that must be swiped left to trigger deletion. */
        const val CONFIRM_RATIO = 0.45f
    }
}
