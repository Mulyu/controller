package com.mulyu.controller.core.pad

/** Normalized stick position, both axes in `-1f..1f`, center at `(0, 0)`. */
data class StickVector(val x: Float, val y: Float) {
    companion object {
        val CENTER = StickVector(0f, 0f)
    }
}

/** D-pad direction, reported as an ABS_HAT0X/ABS_HAT0Y pair in `-1..1`. */
enum class DpadDirection(val hatX: Int, val hatY: Int) {
    NONE(0, 0),
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0),
    UP_LEFT(-1, -1),
    UP_RIGHT(1, -1),
    DOWN_LEFT(-1, 1),
    DOWN_RIGHT(1, 1),
}

/**
 * Full logical state of the virtual gamepad at one instant. This is the
 * output of the overlay UI and the input to [PadEventTranslator], which
 * turns state transitions into uinput events.
 */
data class PadState(
    val buttons: Set<PadButton> = emptySet(),
    val leftStick: StickVector = StickVector.CENTER,
    val rightStick: StickVector = StickVector.CENTER,
    /** 0f (released) to 1f (fully pulled). */
    val leftTrigger: Float = 0f,
    /** 0f (released) to 1f (fully pulled). */
    val rightTrigger: Float = 0f,
    val dpad: DpadDirection = DpadDirection.NONE,
) {
    companion object {
        val NEUTRAL = PadState()
    }
}
