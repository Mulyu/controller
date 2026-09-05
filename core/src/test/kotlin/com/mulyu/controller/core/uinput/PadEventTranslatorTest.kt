package com.mulyu.controller.core.uinput

import com.mulyu.controller.core.pad.DpadDirection
import com.mulyu.controller.core.pad.PadButton
import com.mulyu.controller.core.pad.PadState
import com.mulyu.controller.core.pad.StickVector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PadEventTranslatorTest {

    private val profile = Xbox360Profile.profile

    @Test
    fun `identical states produce no events`() {
        val events = PadEventTranslator.diff(PadState.NEUTRAL, PadState.NEUTRAL, profile)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `pressing a button emits key down then syn`() {
        val next = PadState(buttons = setOf(PadButton.A))
        val events = PadEventTranslator.diff(PadState.NEUTRAL, next, profile)

        assertEquals(
            listOf(
                UinputEvent(EvdevType.EV_KEY, "BTN_A", 1),
                UinputEvent(EvdevType.EV_SYN, EvdevSyn.SYN_REPORT, 0),
            ),
            events,
        )
    }

    @Test
    fun `releasing a button emits key up then syn`() {
        val previous = PadState(buttons = setOf(PadButton.A))
        val events = PadEventTranslator.diff(previous, PadState.NEUTRAL, profile)

        assertEquals(
            listOf(
                UinputEvent(EvdevType.EV_KEY, "BTN_A", 0),
                UinputEvent(EvdevType.EV_SYN, EvdevSyn.SYN_REPORT, 0),
            ),
            events,
        )
    }

    @Test
    fun `moving left stick emits both axes and syn`() {
        val next = PadState(leftStick = StickVector(1f, -1f))
        val events = PadEventTranslator.diff(PadState.NEUTRAL, next, profile)

        assertEquals(
            listOf(
                UinputEvent(EvdevType.EV_ABS, "ABS_X", 32767),
                UinputEvent(EvdevType.EV_ABS, "ABS_Y", -32767),
                UinputEvent(EvdevType.EV_SYN, EvdevSyn.SYN_REPORT, 0),
            ),
            events,
        )
    }

    @Test
    fun `unchanged axis is not re-sent`() {
        val state = PadState(leftStick = StickVector(0.5f, 0f))
        val events = PadEventTranslator.diff(state, state.copy(rightTrigger = 1f), profile)

        assertEquals(
            listOf(
                UinputEvent(EvdevType.EV_ABS, "ABS_RZ", 255),
                UinputEvent(EvdevType.EV_SYN, EvdevSyn.SYN_REPORT, 0),
            ),
            events,
        )
    }

    @Test
    fun `dpad direction change emits hat axes`() {
        val next = PadState(dpad = DpadDirection.UP_RIGHT)
        val events = PadEventTranslator.diff(PadState.NEUTRAL, next, profile)

        assertEquals(
            listOf(
                UinputEvent(EvdevType.EV_ABS, "ABS_HAT0X", 1),
                UinputEvent(EvdevType.EV_ABS, "ABS_HAT0Y", -1),
                UinputEvent(EvdevType.EV_SYN, EvdevSyn.SYN_REPORT, 0),
            ),
            events,
        )
    }

    @Test
    fun `multiple simultaneous button changes are all emitted before syn`() {
        val next = PadState(buttons = setOf(PadButton.A, PadButton.B, PadButton.START))
        val events = PadEventTranslator.diff(PadState.NEUTRAL, next, profile)

        assertEquals(EvdevType.EV_SYN, events.last().type)
        val keyEvents = events.dropLast(1)
        assertEquals(3, keyEvents.size)
        assertTrue(keyEvents.all { it.type == EvdevType.EV_KEY && it.value == 1 })
        assertEquals(setOf("BTN_A", "BTN_B", "BTN_START"), keyEvents.map { it.code }.toSet())
    }
}
