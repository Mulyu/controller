package com.mulyu.controller.shizuku

import java.io.BufferedWriter
import java.io.OutputStreamWriter

/**
 * Runs in a separate process owned by shell (or root, if the user granted
 * Shizuku via root) UID, launched by [rikka.shizuku.Shizuku.bindUserService].
 * Code here executes with shell/root privilege, which is what actually lets
 * it open `/dev/uinput` -- the main app process (a normal app UID) cannot.
 *
 * v0 approach: shell out to the `uinput` command-line tool (bundled with
 * AOSP) rather than calling `ioctl(UI_DEV_CREATE, ...)` via JNI. This keeps
 * the PoC free of native code; a JNI-based backend talking to `/dev/uinput`
 * directly is the planned v1 upgrade for lower latency (see README).
 */
class UinputUserService : IUinputService.Stub() {

    private var process: Process? = null
    private var stdin: BufferedWriter? = null

    override fun start(registerCommandJson: String): Boolean {
        return try {
            val proc = ProcessBuilder("uinput", "-")
                .redirectErrorStream(true)
                .start()
            process = proc
            stdin = BufferedWriter(OutputStreamWriter(proc.outputStream))
            writeLine(registerCommandJson)
            proc.isAlive
        } catch (t: Throwable) {
            destroy()
            false
        }
    }

    override fun sendCommand(commandJson: String) {
        writeLine(commandJson)
    }

    override fun isAlive(): Boolean = process?.isAlive == true

    override fun destroy() {
        runCatching { stdin?.close() }
        runCatching { process?.destroy() }
        stdin = null
        process = null
    }

    private fun writeLine(json: String) {
        val writer = stdin ?: return
        runCatching {
            writer.write(json)
            writer.write("\n")
            writer.flush()
        }
        // A write failure most likely means the uinput process died; the
        // caller can observe that via isAlive() and restart the backend.
    }
}
