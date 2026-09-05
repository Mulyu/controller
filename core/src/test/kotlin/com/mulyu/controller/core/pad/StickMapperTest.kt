package com.mulyu.controller.core.pad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StickMapperTest {

    @Test
    fun `no displacement maps to center`() {
        val result = StickMapper.mapTouch(dx = 0f, dy = 0f, maxRadius = 100f)
        assertEquals(StickVector.CENTER, result)
    }

    @Test
    fun `displacement within deadzone maps to center`() {
        val result = StickMapper.mapTouch(dx = 5f, dy = 0f, maxRadius = 100f, deadZone = 0.12f)
        assertEquals(StickVector.CENTER, result)
    }

    @Test
    fun `full displacement reaches magnitude 1`() {
        val result = StickMapper.mapTouch(dx = 100f, dy = 0f, maxRadius = 100f)
        assertEquals(1f, result.x, 1e-4f)
        assertEquals(0f, result.y, 1e-4f)
    }

    @Test
    fun `displacement beyond radius is clamped to magnitude 1`() {
        val result = StickMapper.mapTouch(dx = 300f, dy = 0f, maxRadius = 100f)
        assertEquals(1f, result.x, 1e-4f)
    }

    @Test
    fun `diagonal displacement preserves direction`() {
        val result = StickMapper.mapTouch(dx = 100f, dy = 100f, maxRadius = 141.42f, deadZone = 0f)
        assertEquals(result.x, result.y, 1e-3f)
        val magnitude = kotlin.math.sqrt(result.x * result.x + result.y * result.y)
        assertTrue(magnitude in 0.99f..1.01f)
    }

    @Test
    fun `deadzone rescales remaining travel to still reach 1`() {
        val justOutsideDeadzone = StickMapper.mapTouch(dx = 12.01f, dy = 0f, maxRadius = 100f, deadZone = 0.12f)
        assertTrue(justOutsideDeadzone.x > 0f && justOutsideDeadzone.x < 0.05f)

        val full = StickMapper.mapTouch(dx = 100f, dy = 0f, maxRadius = 100f, deadZone = 0.12f)
        assertEquals(1f, full.x, 1e-4f)
    }

    @Test
    fun `toSigned16 maps normalized range to axis range`() {
        assertEquals(0, StickMapper.toSigned16(0f))
        assertEquals(32767, StickMapper.toSigned16(1f))
        assertEquals(-32767, StickMapper.toSigned16(-1f))
        assertEquals(32767, StickMapper.toSigned16(5f)) // out-of-range input is clamped
        assertEquals(-32767, StickMapper.toSigned16(-5f))
    }

    @Test
    fun `toUnsigned8 maps normalized range to trigger range`() {
        assertEquals(0, StickMapper.toUnsigned8(0f))
        assertEquals(255, StickMapper.toUnsigned8(1f))
        assertEquals(0, StickMapper.toUnsigned8(-1f)) // clamped
        assertEquals(255, StickMapper.toUnsigned8(2f)) // clamped
    }
}
