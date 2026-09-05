package com.mulyu.controller.core.uinput

import com.mulyu.controller.core.pad.PadButton

/** One ABS axis the virtual device advertises during registration. */
data class AbsAxisSpec(
    val code: String,
    val minimum: Int,
    val maximum: Int,
    /** Kept at 0 -- deadzone is applied on the input side, see StickMapper. */
    val flat: Int = 0,
    val fuzz: Int = 0,
    val resolution: Int = 0,
)

/**
 * Describes a uinput gamepad device: identity (name/vendor/product/bus),
 * which evdev key code each [PadButton] maps to, which ABS axis carries each
 * stick/trigger/dpad channel, and the full axis list to advertise at
 * registration time.
 */
data class GamepadProfile(
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val bus: String,
    val buttonKeyCodes: Map<PadButton, String>,
    val leftStickXCode: String,
    val leftStickYCode: String,
    val rightStickXCode: String,
    val rightStickYCode: String,
    val leftTriggerCode: String,
    val rightTriggerCode: String,
    val dpadXCode: String,
    val dpadYCode: String,
    val absAxes: List<AbsAxisSpec>,
)

/**
 * An Xbox 360 wired pad profile. Chosen because Android ships
 * `Vendor_045e_Product_028e.kl`/`.idc` out of the box, so games that
 * whitelist known gamepads (rather than relying on generic
 * `InputDevice.SOURCE_GAMEPAD` detection) are more likely to recognize it.
 *
 * `BTN_MODE` (the guide button) is intentionally not exposed -- it can
 * trigger system-level behavior on some devices.
 */
object Xbox360Profile {
    private const val STICK_MIN = -32768
    private const val STICK_MAX = 32767
    private const val TRIGGER_MIN = 0
    private const val TRIGGER_MAX = 255
    private const val HAT_MIN = -1
    private const val HAT_MAX = 1

    const val ABS_X = "ABS_X"
    const val ABS_Y = "ABS_Y"
    const val ABS_RX = "ABS_RX"
    const val ABS_RY = "ABS_RY"
    const val ABS_Z = "ABS_Z"
    const val ABS_RZ = "ABS_RZ"
    const val ABS_HAT0X = "ABS_HAT0X"
    const val ABS_HAT0Y = "ABS_HAT0Y"

    val profile: GamepadProfile = GamepadProfile(
        name = "Microsoft X-Box 360 pad",
        vendorId = 0x045e,
        productId = 0x028e,
        bus = "usb",
        buttonKeyCodes = mapOf(
            PadButton.A to "BTN_A",
            PadButton.B to "BTN_B",
            PadButton.X to "BTN_X",
            PadButton.Y to "BTN_Y",
            PadButton.L1 to "BTN_TL",
            PadButton.R1 to "BTN_TR",
            PadButton.THUMB_L to "BTN_THUMBL",
            PadButton.THUMB_R to "BTN_THUMBR",
            PadButton.SELECT to "BTN_SELECT",
            PadButton.START to "BTN_START",
        ),
        leftStickXCode = ABS_X,
        leftStickYCode = ABS_Y,
        rightStickXCode = ABS_RX,
        rightStickYCode = ABS_RY,
        leftTriggerCode = ABS_Z,
        rightTriggerCode = ABS_RZ,
        dpadXCode = ABS_HAT0X,
        dpadYCode = ABS_HAT0Y,
        absAxes = listOf(
            AbsAxisSpec(ABS_X, STICK_MIN, STICK_MAX),
            AbsAxisSpec(ABS_Y, STICK_MIN, STICK_MAX),
            AbsAxisSpec(ABS_RX, STICK_MIN, STICK_MAX),
            AbsAxisSpec(ABS_RY, STICK_MIN, STICK_MAX),
            AbsAxisSpec(ABS_Z, TRIGGER_MIN, TRIGGER_MAX),
            AbsAxisSpec(ABS_RZ, TRIGGER_MIN, TRIGGER_MAX),
            AbsAxisSpec(ABS_HAT0X, HAT_MIN, HAT_MAX),
            AbsAxisSpec(ABS_HAT0Y, HAT_MIN, HAT_MAX),
        ),
    )
}
