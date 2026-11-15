package org.pilou.bettersony.widget

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.pilou.bettersony.alerts.BatteryAlerts
import org.pilou.bettersony.data.HeadsetJob
import org.pilou.bettersony.data.Headsets
import org.pilou.bettersony.data.ListenMode
import org.pilou.bettersony.data.isSony

private const val TAG = "BetterSony"
private const val BT_SETTLE_MS = 3000L

/** Mode button on a widget: goes through the shared link, so it never fights the app for the channel. */
class SetModeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val mode = parameters[MODE_KEY]?.let { name -> ListenMode.entries.firstOrNull { it.name == name } } ?: return
        val before = WidgetStore.state.value
        if (!before.connected) return
        WidgetStore.patch(context) { it.copy(mode = mode) }
        val sent = runCatching { Headsets.setModeFromWidget(context, mode) }
            .onFailure { Log.w(TAG, "widget mode switch failed", it) }
            .getOrDefault(false)
        if (!sent) WidgetStore.patch(context) { it.copy(mode = before.mode) }
    }

    companion object {
        val MODE_KEY = ActionParameters.Key<String>("mode")
    }
}

/** Power button: handed to a job so it completes even if the widget host lets go of us. */
class PowerOffAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        if (!WidgetStore.state.value.connected) return
        WidgetStore.patch(context) { it.copy(turningOff = true) }
        HeadsetJob.schedule(context, HeadsetJob.Task.POWER_OFF)
    }
}

/**
 * Headphones link up or down (ACL broadcasts reach manifest receivers, so this runs with the app closed):
 * restore settings on connect, redraw widgets, re-arm the low battery reminder.
 */
class BluetoothChangeReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        val classic = intent.getIntExtra(BluetoothDevice.EXTRA_TRANSPORT, BluetoothDevice.TRANSPORT_BREDR) == BluetoothDevice.TRANSPORT_BREDR
        val sony = device != null && classic && runCatching { isSony(device) }.getOrDefault(false)
        val headsetBack = sony && intent.action == BluetoothDevice.ACTION_ACL_CONNECTED
        if (headsetBack) {
            BatteryAlerts.rearm(context)
            HeadsetJob.schedule(context, HeadsetJob.Task.ON_CONNECT)
        }
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                delay(BT_SETTLE_MS)
                WidgetStore.refresh(context)
            } finally {
                pending.finish()
            }
        }
    }
}
