package com.mulyu.controller

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mulyu.controller.overlay.OverlayService
import com.mulyu.controller.ui.ControllerScreen
import rikka.shizuku.Shizuku

/**
 * Permission setup + a "pad tester" that reads back gamepad input while this
 * screen is focused, so the virtual gamepad end-to-end path can be verified
 * without needing a real game installed.
 */
class MainActivity : ComponentActivity() {

    private var shizukuAvailable by mutableStateOf(false)
    private var shizukuGranted by mutableStateOf(false)
    private var overlayGranted by mutableStateOf(false)
    private var notificationsGranted by mutableStateOf(true)

    private var lastDeviceName by mutableStateOf<String?>(null)
    private var lastAxisSummary by mutableStateOf("")
    private var lastKeySummary by mutableStateOf("")

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationsGranted = granted
        }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { refreshShizukuState() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { shizukuAvailable = false }
    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            shizukuGranted = grantResult == PackageManager.PERMISSION_GRANTED
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val shizukuLabel = when {
                        !shizukuAvailable -> "Shizuku: not running"
                        !shizukuGranted -> "Shizuku: permission not granted"
                        else -> "Shizuku: ready"
                    }
                    ControllerScreen(
                        shizukuLabel = shizukuLabel,
                        shizukuReady = shizukuAvailable && shizukuGranted,
                        overlayGranted = overlayGranted,
                        notificationsGranted = notificationsGranted,
                        lastDeviceName = lastDeviceName,
                        lastAxisSummary = lastAxisSummary,
                        lastKeySummary = lastKeySummary,
                        onRequestShizuku = ::requestShizukuPermission,
                        onRequestOverlay = ::requestOverlayPermission,
                        onRequestNotifications = ::requestNotificationPermission,
                        onStartOverlay = ::startOverlay,
                        onStopOverlay = ::stopOverlay,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshShizukuState()
        overlayGranted = Settings.canDrawOverlays(this)
        notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        super.onDestroy()
    }

    private fun refreshShizukuState() {
        shizukuAvailable = Shizuku.pingBinder()
        shizukuGranted = shizukuAvailable && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    private fun requestShizukuPermission() {
        if (!Shizuku.pingBinder() || Shizuku.isPreV11()) return
        Shizuku.requestPermission(1)
    }

    private fun requestOverlayPermission() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startOverlay() {
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopOverlay() {
        stopService(Intent(this, OverlayService::class.java))
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
            lastDeviceName = InputDevice.getDevice(event.deviceId)?.name
            lastAxisSummary = buildString {
                append("X=").append("%.2f".format(event.getAxisValue(MotionEvent.AXIS_X)))
                append(" Y=").append("%.2f".format(event.getAxisValue(MotionEvent.AXIS_Y)))
                append(" Z=").append("%.2f".format(event.getAxisValue(MotionEvent.AXIS_Z)))
                append(" RZ=").append("%.2f".format(event.getAxisValue(MotionEvent.AXIS_RZ)))
                append(" HAT_X=").append("%.2f".format(event.getAxisValue(MotionEvent.AXIS_HAT_X)))
                append(" HAT_Y=").append("%.2f".format(event.getAxisValue(MotionEvent.AXIS_HAT_Y)))
            }
        }
        return super.dispatchGenericMotionEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) {
            lastDeviceName = InputDevice.getDevice(event.deviceId)?.name
            val actionLabel = if (event.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"
            lastKeySummary = "${KeyEvent.keyCodeToString(event.keyCode)} $actionLabel"
        }
        return super.dispatchKeyEvent(event)
    }
}
