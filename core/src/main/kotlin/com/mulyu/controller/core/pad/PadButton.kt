package com.mulyu.controller.core.pad

/**
 * Digital buttons exposed by the virtual gamepad. D-pad is modeled separately
 * as [DpadDirection] because it is reported as a hat axis (ABS_HAT0X/Y), not
 * discrete keys, on the Xbox 360 pad profile this PoC emulates.
 */
enum class PadButton {
    A,
    B,
    X,
    Y,
    L1,
    R1,
    THUMB_L,
    THUMB_R,
    SELECT,
    START,
}
