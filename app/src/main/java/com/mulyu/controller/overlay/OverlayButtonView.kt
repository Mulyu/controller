package com.mulyu.controller.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View

/** A single round face button: draws a label, reports press/release. */
class OverlayButtonView(context: Context, private val label: String) : View(context) {

    var onPressedChanged: ((Boolean) -> Unit)? = null

    private var pressed = false

    private val idlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x40FFFFFF
        style = Paint.Style.FILL
    }
    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xA0FFFFFF.toInt()
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        textSize = 40f
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) / 2f
        canvas.drawCircle(cx, cy, radius, if (pressed) pressedPaint else idlePaint)
        val textY = cy - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(label, cx, textY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> setPressedState(true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> setPressedState(false)
        }
        return true
    }

    private fun setPressedState(value: Boolean) {
        if (pressed == value) return
        pressed = value
        invalidate()
        onPressedChanged?.invoke(value)
    }
}
