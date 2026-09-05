package com.mulyu.controller.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.mulyu.controller.BuildConfig
import com.mulyu.controller.core.pad.PadState
import com.mulyu.controller.core.uinput.PadEventTranslator
import com.mulyu.controller.core.uinput.UinputCommandBuilder
import com.mulyu.controller.core.uinput.Xbox360Profile
import rikka.shizuku.Shizuku

private const val SHIZUKU_PERMISSION_REQUEST_CODE = 5115

/** Connection/permission state of the Shizuku-backed uinput device. */
sealed interface BackendState {
    /** Shizuku app/service is not running on this device. */
    data object ShizukuUnavailable : BackendState

    /** Shizuku is running but this app hasn't been granted access yet. */
    data object PermissionRequired : BackendState

    /** Permission granted, binding the privileged UserService. */
    data object Connecting : BackendState

    /** uinput device is registered and accepting reports. */
    data object Ready : BackendState

    data class Failed(val reason: String) : BackendState
}

/**
 * Owns the Shizuku UserService connection and the virtual Xbox 360 pad it
 * hosts. Callers feed [PadState] snapshots (from the overlay UI); only the
 * diff from the previous snapshot is actually sent over the AIDL binder, via
 * [PadEventTranslator].
 */
class ShizukuGamepadBackend(private val context: Context) {

    private var uinputService: IUinputService? = null
    private var previousState: PadState = PadState.NEUTRAL
    private var nextCommandId = 1

    var onStateChanged: ((BackendState) -> Unit)? = null

    var currentState: BackendState = BackendState.ShizukuUnavailable
        private set

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, UinputUserService::class.java.name),
    )
        .daemon(false)
        .processNameSuffix("uinput")
        .debuggable(BuildConfig.DEBUG)
        .version(1)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = IUinputService.Stub.asInterface(binder)
            uinputService = service
            val registerJson = UinputCommandBuilder.registerCommand(Xbox360Profile.profile)
            updateState(
                if (service.start(registerJson)) {
                    previousState = PadState.NEUTRAL
                    BackendState.Ready
                } else {
                    BackendState.Failed("uinput process failed to start on this device")
                },
            )
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            uinputService = null
            updateState(BackendState.ShizukuUnavailable)
        }
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    bind()
                } else {
                    updateState(BackendState.PermissionRequired)
                }
            }
        }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { refresh() }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        uinputService = null
        updateState(BackendState.ShizukuUnavailable)
    }

    /** Registers Shizuku listeners and attempts to connect. Call once, e.g. from a Service's onCreate. */
    fun start() {
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        refresh()
    }

    /** Tears down the uinput device and Shizuku listeners. */
    fun stop() {
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        runCatching { uinputService?.destroy() }
        runCatching { Shizuku.unbindUserService(userServiceArgs, connection, true) }
        uinputService = null
    }

    fun refresh() {
        if (!Shizuku.pingBinder()) {
            updateState(BackendState.ShizukuUnavailable)
            return
        }
        if (Shizuku.isPreV11()) {
            updateState(BackendState.Failed("Shizuku version too old (needs API v11+)"))
            return
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            updateState(BackendState.PermissionRequired)
            return
        }
        updateState(BackendState.Connecting)
        bind()
    }

    fun requestPermission() {
        if (!Shizuku.pingBinder() || Shizuku.isPreV11()) return
        Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
    }

    private fun bind() {
        Shizuku.bindUserService(userServiceArgs, connection)
    }

    /** Sends only the events needed to move from the previous state to [next]. */
    fun sendPadState(next: PadState) {
        val service = uinputService ?: return
        val events = PadEventTranslator.diff(previousState, next, Xbox360Profile.profile)
        if (events.isEmpty()) return
        previousState = next
        nextCommandId++
        runCatching { service.sendCommand(UinputCommandBuilder.injectCommand(events, nextCommandId)) }
    }

    private fun updateState(state: BackendState) {
        currentState = state
        onStateChanged?.invoke(state)
    }
}
