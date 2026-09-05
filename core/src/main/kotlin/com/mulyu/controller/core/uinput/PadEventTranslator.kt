package com.mulyu.controller.core.uinput

import com.mulyu.controller.core.pad.PadButton
import com.mulyu.controller.core.pad.PadState
import com.mulyu.controller.core.pad.StickMapper

/**
 * Turns a [PadState] transition into the minimal list of uinput events
 * needed to bring the kernel device up to date, terminated by `SYN_REPORT`.
 *
 * Only changed fields are emitted -- an unchanged [PadState] produces an
 * empty list, so callers can poll on every frame without flooding uinput
 * with redundant reports.
 */
object PadEventTranslator {

    fun diff(previous: PadState, next: PadState, profile: GamepadProfile): List<UinputEvent> {
        val events = mutableListOf<UinputEvent>()

        for (button in PadButton.entries) {
            val code = profile.buttonKeyCodes[button] ?: continue
            val wasPressed = button in previous.buttons
            val isPressed = button in next.buttons
            if (wasPressed != isPressed) {
                events += UinputEvent(EvdevType.EV_KEY, code, if (isPressed) 1 else 0)
            }
        }

        addAxisIfChanged(events, profile.leftStickXCode, previous.leftStick.x, next.leftStick.x, StickMapper::toSigned16)
        addAxisIfChanged(events, profile.leftStickYCode, previous.leftStick.y, next.leftStick.y, StickMapper::toSigned16)
        addAxisIfChanged(events, profile.rightStickXCode, previous.rightStick.x, next.rightStick.x, StickMapper::toSigned16)
        addAxisIfChanged(events, profile.rightStickYCode, previous.rightStick.y, next.rightStick.y, StickMapper::toSigned16)
        addAxisIfChanged(events, profile.leftTriggerCode, previous.leftTrigger, next.leftTrigger, StickMapper::toUnsigned8)
        addAxisIfChanged(events, profile.rightTriggerCode, previous.rightTrigger, next.rightTrigger, StickMapper::toUnsigned8)

        if (previous.dpad.hatX != next.dpad.hatX) {
            events += UinputEvent(EvdevType.EV_ABS, profile.dpadXCode, next.dpad.hatX)
        }
        if (previous.dpad.hatY != next.dpad.hatY) {
            events += UinputEvent(EvdevType.EV_ABS, profile.dpadYCode, next.dpad.hatY)
        }

        if (events.isEmpty()) return emptyList()
        events += UinputEvent(EvdevType.EV_SYN, EvdevSyn.SYN_REPORT, 0)
        return events
    }

    private inline fun addAxisIfChanged(
        events: MutableList<UinputEvent>,
        code: String,
        previous: Float,
        next: Float,
        scale: (Float) -> Int,
    ) {
        if (previous != next) {
            events += UinputEvent(EvdevType.EV_ABS, code, scale(next))
        }
    }
}
