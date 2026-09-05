package com.mulyu.controller.core.uinput

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UinputCommandBuilderTest {

    private val profile = Xbox360Profile.profile

    @Test
    fun `register command carries device identity`() {
        val json = UinputCommandBuilder.registerCommand(profile, id = 7)

        assertTrue(json.contains("\"id\":7"))
        assertTrue(json.contains("\"command\":\"register\""))
        assertTrue(json.contains("\"name\":\"Microsoft X-Box 360 pad\""))
        assertTrue(json.contains("\"vid\":${0x045e}"))
        assertTrue(json.contains("\"pid\":${0x028e}"))
        assertTrue(json.contains("\"bus\":\"usb\""))
    }

    @Test
    fun `register command lists every button key code`() {
        val json = UinputCommandBuilder.registerCommand(profile)

        for (code in profile.buttonKeyCodes.values) {
            assertTrue("expected $code in $json", json.contains("\"$code\""))
        }
    }

    @Test
    fun `register command lists abs_info for every axis with its range`() {
        val json = UinputCommandBuilder.registerCommand(profile)

        assertTrue(json.contains("\"code\":\"ABS_X\""))
        assertTrue(json.contains("\"minimum\":-32768"))
        assertTrue(json.contains("\"maximum\":32767"))
        assertTrue(json.contains("\"code\":\"ABS_Z\""))
        assertTrue(json.contains("\"maximum\":255"))
    }

    @Test
    fun `inject command flattens events into type,code,value triples`() {
        val events = listOf(
            UinputEvent(EvdevType.EV_KEY, "BTN_A", 1),
            UinputEvent(EvdevType.EV_SYN, EvdevSyn.SYN_REPORT, 0),
        )

        val json = UinputCommandBuilder.injectCommand(events, id = 3)

        assertEquals(
            "{\"id\":3,\"command\":\"inject\",\"events\":[\"EV_KEY\",\"BTN_A\",1,\"EV_SYN\",\"SYN_REPORT\",0]}",
            json,
        )
    }

    @Test
    fun `string values are escaped`() {
        val weirdProfile = profile.copy(name = "pad \"with\" quotes\\slash")
        val json = UinputCommandBuilder.registerCommand(weirdProfile)
        assertTrue(json.contains("\"name\":\"pad \\\"with\\\" quotes\\\\slash\""))
    }
}
