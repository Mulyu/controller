package com.mulyu.controller.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

/**
 * Small always-visible handle used to pause/resume input capture (e.g. to
 * interact with the game's own menus or an OS dialog) without stopping the
 * overlay service. Fires [onToggle] on a tap; ignores drags so it doesn't
 * fight with [StickView]-style controls.
 */
class ToggleHandleView(context: Context) : View(context) {

    var onToggle: (() -> Unit)? = null

    private var downX = 0f
    private var downY = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x60FFFFFF
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(width / 2f, height / 2f, minOf(width, height) / 2f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
            }
            MotionEvent.ACTION_UP -> {
                if (hypot(event.rawX - downX, event.rawY - downY) < 20f) {
                    onToggle?.invoke()
                }
            }
        }
        return true
    }
}
