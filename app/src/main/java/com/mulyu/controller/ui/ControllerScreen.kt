package com.mulyu.controller.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ControllerScreen(
    shizukuLabel: String,
    shizukuReady: Boolean,
    overlayGranted: Boolean,
    notificationsGranted: Boolean,
    lastDeviceName: String?,
    lastAxisSummary: String,
    lastKeySummary: String,
    onRequestShizuku: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestNotifications: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
) {
    val readyToStart = shizukuReady && overlayGranted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Virtual Controller (PoC)", style = MaterialTheme.typography.headlineSmall)

        PermissionRow(label = shizukuLabel, granted = shizukuReady, actionLabel = "Request", onClick = onRequestShizuku)
        if (!shizukuReady) {
            Text(
                "Install and start the Shizuku app first (via ADB wireless debugging, " +
                    "or root), then tap Request.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        PermissionRow(
            label = if (overlayGranted) "Overlay permission: granted" else "Overlay permission: not granted",
            granted = overlayGranted,
            actionLabel = "Grant",
            onClick = onRequestOverlay,
        )
        PermissionRow(
            label = if (notificationsGranted) "Notifications: granted" else "Notifications: not granted",
            granted = notificationsGranted,
            actionLabel = "Grant",
            onClick = onRequestNotifications,
        )

        HorizontalDivider()

        Button(onClick = onStartOverlay, enabled = readyToStart, modifier = Modifier.fillMaxWidth()) {
            Text("Start overlay")
        }
        OutlinedButton(onClick = onStopOverlay, modifier = Modifier.fillMaxWidth()) {
            Text("Stop overlay")
        }
        if (!readyToStart) {
            Text(
                "Grant Shizuku + overlay permissions above before starting.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        HorizontalDivider()

        Text("Pad tester", style = MaterialTheme.typography.titleMedium)
        Text("Detected device: ${lastDeviceName ?: "none yet"}")
        Text("Axes: ${lastAxisSummary.ifBlank { "no motion events yet" }}")
        Text("Last key: ${lastKeySummary.ifBlank { "none yet" }}")
        Text(
            "Start the overlay, then move the stick / press a face button while " +
                "this screen is focused. If \"Detected device\" shows the Xbox 360 " +
                "pad, the uinput device is alive end-to-end.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, actionLabel: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        if (!granted) {
            TextButton(onClick = onClick) { Text(actionLabel) }
        }
    }
}
