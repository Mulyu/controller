package com.mulyu.controller.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.mulyu.controller.MainActivity
import com.mulyu.controller.R
import com.mulyu.controller.core.pad.PadButton
import com.mulyu.controller.core.pad.PadState
import com.mulyu.controller.core.pad.StickVector
import com.mulyu.controller.shizuku.ShizukuGamepadBackend

/**
 * Foreground service that hosts the floating stick/buttons and forwards
 * their state to the virtual gamepad via [ShizukuGamepadBackend].
 *
 * Each control is added as its own small [WindowManager] window rather than
 * one full-screen overlay. A full-screen touchable window would swallow
 * every touch in its bounds -- including taps meant for the game underneath
 * -- while a full-screen `FLAG_NOT_TOUCHABLE` window would swallow none.
 * Small per-control windows let untouched screen area pass straight through
 * to the app below with no extra flag juggling.
 */
class OverlayService : Service() {

    companion object {
        private const val CHANNEL_ID = "overlay"
        private const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.mulyu.controller.action.STOP_OVERLAY"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var backend: ShizukuGamepadBackend

    private val controlViews = mutableListOf<View>()
    private var handleView: View? = null
    private var captureEnabled = true

    private var leftStick = StickVector.CENTER
    private val heldButtons = mutableSetOf<PadButton>()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        backend = ShizukuGamepadBackend(applicationContext).also { it.start() }
        startForegroundCompat(buildNotification())
        addControls()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        (controlViews + listOfNotNull(handleView)).forEach { runCatching { windowManager.removeView(it) } }
        controlViews.clear()
        handleView = null
        backend.stop()
        super.onDestroy()
    }

    private fun addControls() {
        val density = resources.displayMetrics.density
        val stickSize = (176 * density).toInt()
        val buttonSize = (56 * density).toInt()
        val handleSize = (36 * density).toInt()
        val margin = (16 * density).toInt()
        val buttonSpacing = (buttonSize * 1.15f).toInt()

        val stick = StickView(this).also { view ->
            view.onStickChanged = { vector ->
                leftStick = vector
                publish()
            }
        }
        addControlView(stick, stickSize, stickSize, Gravity.BOTTOM or Gravity.START, margin, margin)

        addFaceButton(PadButton.Y, "Y", buttonSize, margin + buttonSpacing, margin + buttonSpacing * 2)
        addFaceButton(PadButton.A, "A", buttonSize, margin + buttonSpacing, margin)
        addFaceButton(PadButton.X, "X", buttonSize, margin, margin + buttonSpacing)
        addFaceButton(PadButton.B, "B", buttonSize, margin + buttonSpacing * 2, margin + buttonSpacing)

        val handle = ToggleHandleView(this).also { it.onToggle = { toggleCapture() } }
        addOverlayWindow(handle, handleSize, handleSize, Gravity.TOP or Gravity.END, margin, margin)
        handleView = handle
    }

    private fun addFaceButton(button: PadButton, label: String, size: Int, marginEnd: Int, marginBottom: Int) {
        val view = OverlayButtonView(this, label).also { v ->
            v.onPressedChanged = { pressed ->
                if (pressed) heldButtons.add(button) else heldButtons.remove(button)
                publish()
            }
        }
        addControlView(view, size, size, Gravity.BOTTOM or Gravity.END, marginEnd, marginBottom)
    }

    private fun addControlView(view: View, width: Int, height: Int, gravity: Int, marginX: Int, marginY: Int) {
        addOverlayWindow(view, width, height, gravity, marginX, marginY)
        controlViews += view
    }

    private fun addOverlayWindow(view: View, width: Int, height: Int, gravity: Int, marginX: Int, marginY: Int) {
        val params = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            this.gravity = gravity
            x = marginX
            y = marginY
        }
        windowManager.addView(view, params)
    }

    private fun toggleCapture() {
        captureEnabled = !captureEnabled
        val visibility = if (captureEnabled) View.VISIBLE else View.GONE
        controlViews.forEach { it.visibility = visibility }
        if (!captureEnabled) {
            leftStick = StickVector.CENTER
            heldButtons.clear()
            publish()
        }
    }

    private fun publish() {
        backend.sendPadState(PadState(buttons = heldButtons.toSet(), leftStick = leftStick))
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.overlay_notification_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .addAction(Notification.Action.Builder(null, getString(R.string.overlay_notification_stop), stopIntent).build())
            .setOngoing(true)
            .build()
    }
}
