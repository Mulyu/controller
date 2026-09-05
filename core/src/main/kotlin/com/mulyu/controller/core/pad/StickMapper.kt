package com.mulyu.controller.core.pad

import kotlin.math.min
import kotlin.math.sqrt

/**
 * Converts raw touch offsets from an overlay stick control into a normalized
 * [StickVector], and normalized values into the integer axis ranges the
 * uinput device descriptor advertises.
 *
 * Deadzone and clamping happen here (on the "input" side) rather than in the
 * uinput layer, so the uinput device itself can advertise `flat = 0` and stay
 * fully linear -- see [com.mulyu.controller.core.uinput.Xbox360Profile].
 */
object StickMapper {

    /**
     * Maps a raw touch displacement from the stick's center to a normalized
     * [StickVector].
     *
     * @param dx raw horizontal displacement from center, in pixels/dp.
     * @param dy raw vertical displacement from center, in the same unit.
     * @param maxRadius displacement at which the stick is considered fully
     *   deflected (i.e. magnitude 1.0); typically the visual radius of the
     *   stick's travel area.
     * @param deadZone fraction (`0f..1f`) of [maxRadius], measured from
     *   center, that is reported as zero. The remaining travel is rescaled
     *   so the output still reaches exactly 1.0 at [maxRadius].
     */
    fun mapTouch(
        dx: Float,
        dy: Float,
        maxRadius: Float,
        deadZone: Float = 0.12f,
    ): StickVector {
        require(maxRadius > 0f) { "maxRadius must be positive" }
        val magnitude = sqrt(dx * dx + dy * dy)
        if (magnitude <= 1e-6f) return StickVector.CENTER

        val normalizedMagnitude = min(magnitude / maxRadius, 1f)
        if (normalizedMagnitude <= deadZone) return StickVector.CENTER

        val rescaled = (normalizedMagnitude - deadZone) / (1f - deadZone)
        val unitX = dx / magnitude
        val unitY = dy / magnitude
        return StickVector(x = unitX * rescaled, y = unitY * rescaled)
    }

    /** Scales a normalized `-1f..1f` value to a signed 16-bit axis (e.g. ABS_X). */
    fun toSigned16(normalized: Float): Int {
        val clamped = normalized.coerceIn(-1f, 1f)
        return (clamped * 32767f).toInt()
    }

    /** Scales a normalized `0f..1f` value to an unsigned 8-bit axis (e.g. ABS_Z trigger). */
    fun toUnsigned8(normalized: Float): Int {
        val clamped = normalized.coerceIn(0f, 1f)
        return (clamped * 255f).toInt()
    }
}
