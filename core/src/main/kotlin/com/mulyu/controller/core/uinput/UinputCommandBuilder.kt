package com.mulyu.controller.core.uinput

/**
 * Builds JSON command strings for AOSP's `uinput` command-line tool, which
 * reads newline-delimited JSON from stdin and drives `/dev/uinput` on our
 * behalf when run as the shell user (see `UinputUserService` in `:app`).
 *
 * KNOWN GAP: the exact field names below are transcribed from documentation
 * of the AOSP `cmds/uinput` tool's `register`/`inject` commands, evdev
 * type/code names as strings (e.g. `"EV_KEY"`, `"BTN_A"`). This has **not**
 * been exercised against a physical device or emulator in this environment
 * (no Android SDK/device available here) -- before relying on it, confirm
 * the schema `uinput --help` / the AOSP source for the target OS version
 * actually expects, and adjust this file if it differs. See README.md
 * "Known gaps / what still needs on-device verification".
 */
object UinputCommandBuilder {

    fun registerCommand(profile: GamepadProfile, id: Int = 1): String {
        val keyCodes = profile.buttonKeyCodes.values.toList().distinct()
        val absCodes = profile.absAxes.map { it.code }

        val configuration = jArr(
            listOf(
                jObj("type" to jStr("UI_SET_EVBIT"), "data" to jArr(listOf(jStr("EV_KEY")))),
                jObj("type" to jStr("UI_SET_KEYBIT"), "data" to jArr(keyCodes.map(::jStr))),
                jObj("type" to jStr("UI_SET_EVBIT"), "data" to jArr(listOf(jStr("EV_ABS")))),
                jObj("type" to jStr("UI_SET_ABSBIT"), "data" to jArr(absCodes.map(::jStr))),
            ),
        )

        val absInfo = jArr(
            profile.absAxes.map { axis ->
                jObj(
                    "code" to jStr(axis.code),
                    "info" to jObj(
                        "value" to jNum(0),
                        "minimum" to jNum(axis.minimum),
                        "maximum" to jNum(axis.maximum),
                        "fuzz" to jNum(axis.fuzz),
                        "flat" to jNum(axis.flat),
                        "resolution" to jNum(axis.resolution),
                    ),
                )
            },
        )

        return jObj(
            "id" to jNum(id),
            "command" to jStr("register"),
            "name" to jStr(profile.name),
            "vid" to jNum(profile.vendorId),
            "pid" to jNum(profile.productId),
            "bus" to jStr(profile.bus),
            "configuration" to configuration,
            "abs_info" to absInfo,
        )
    }

    fun injectCommand(events: List<UinputEvent>, id: Int = 1): String {
        val flat = mutableListOf<String>()
        for (event in events) {
            flat += jStr(event.type)
            flat += jStr(event.code)
            flat += jNum(event.value)
        }
        return jObj(
            "id" to jNum(id),
            "command" to jStr("inject"),
            "events" to jArr(flat),
        )
    }
}

// Minimal hand-rolled JSON writer -- kept dependency-free since :core has no
// JSON library and the schema here is small and fully controlled by us.

private fun jStr(value: String): String = buildString {
    append('"')
    for (ch in value) {
        when (ch) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            else -> append(ch)
        }
    }
    append('"')
}

private fun jNum(value: Int): String = value.toString()

private fun jArr(items: List<String>): String =
    items.joinToString(prefix = "[", postfix = "]", separator = ",")

private fun jObj(vararg fields: Pair<String, String>): String =
    fields.joinToString(prefix = "{", postfix = "}", separator = ",") { (key, value) -> "${jStr(key)}:$value" }
