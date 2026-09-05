package com.mulyu.controller.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import com.mulyu.controller.core.pad.StickMapper
import com.mulyu.controller.core.pad.StickVector

/**
 * A self-contained circular stick: draws its own base + knob and reports a
 * normalized [StickVector] as the finger moves. Deadzone/clamping is
 * delegated to [StickMapper] so the same logic is exercised by the `:core`
 * unit tests.
 */
class StickView(context: Context) : View(context) {

    var onStickChanged: ((StickVector) -> Unit)? = null

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x40FFFFFF
        style = Paint.Style.FILL
    }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x90FFFFFF.toInt()
        style = Paint.Style.FILL
    }

    private var radius = 0f
    private var knobOffsetX = 0f
    private var knobOffsetY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        radius = minOf(w, h) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawCircle(cx, cy, radius, basePaint)
        canvas.drawCircle(cx + knobOffsetX, cy + knobOffsetY, radius * 0.35f, knobPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cx = width / 2f
        val cy = height / 2f
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val dx = event.x - cx
                val dy = event.y - cy
                val vector = StickMapper.mapTouch(dx, dy, radius)
                knobOffsetX = vector.x * radius
                knobOffsetY = vector.y * radius
                invalidate()
                onStickChanged?.invoke(vector)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                knobOffsetX = 0f
                knobOffsetY = 0f
                invalidate()
                onStickChanged?.invoke(StickVector.CENTER)
            }
        }
        return true
    }
}
