package com.mulyu.controller.core.uinput

/** evdev event type names, as accepted by the `uinput` command-line tool. */
object EvdevType {
    const val EV_KEY = "EV_KEY"
    const val EV_ABS = "EV_ABS"
    const val EV_SYN = "EV_SYN"
}

/** evdev sync codes. */
object EvdevSyn {
    const val SYN_REPORT = "SYN_REPORT"
}

/** One evdev event: `(type, code, value)`, e.g. `(EV_KEY, BTN_A, 1)`. */
data class UinputEvent(val type: String, val code: String, val value: Int)
