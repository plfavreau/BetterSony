package org.pilou.bettersony.widget

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.pilou.bettersony.R
import org.pilou.bettersony.alerts.BatteryAlerts
import org.pilou.bettersony.data.AppPrefs
import org.pilou.bettersony.data.ListenMode
import org.pilou.bettersony.data.SonyDevices

private const val DEBOUNCE_MS = 400L
val QUICK_MODES = listOf(ListenMode.NOISE_CANCELLING, ListenMode.AMBIENT, ListenMode.OFF)

/** Everything a widget renders. Built from the last headset snapshot plus live system state. */
data class WidgetData(
    val connected: Boolean = false,
    val battery: Int? = null,
    val charging: Boolean = false,
    val mode: ListenMode? = null,
    val name: String = "WH-1000XM4",
    val phone: String = "",
    val lowThreshold: Int = 20,
    val turningOff: Boolean = false,
) {
    val low: Boolean get() = battery != null && battery <= lowThreshold && !charging
}

fun modeIcon(mode: ListenMode): Int = when (mode) {
    ListenMode.NOISE_CANCELLING -> R.drawable.wm_nc
    ListenMode.WIND -> R.drawable.wm_wind
    ListenMode.AMBIENT -> R.drawable.wm_ambient
    ListenMode.OFF -> R.drawable.wm_off
}

fun modeShort(mode: ListenMode): String = when (mode) {
    ListenMode.NOISE_CANCELLING -> "NC"
    ListenMode.WIND -> "Wind"
    ListenMode.AMBIENT -> "Ambient"
    ListenMode.OFF -> "Off"
}

/** Shared, observable widget state; widgets collect it so a refresh re-renders open sessions too. */
object WidgetStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(WidgetData())
    val state: StateFlow<WidgetData> = _state.asStateFlow()
    private var pending: Job? = null
    @Volatile var loaded = false
        private set

    /** Debug only: freezes the faked state so real refreshes don't overwrite it. */
    @Volatile var demo = false

    /** Optimistic local change (e.g. a mode tap) shown before the headset confirms. */
    fun patch(context: Context, transform: (WidgetData) -> WidgetData) {
        _state.value = transform(_state.value)
        push(context)
    }

    /** Re-reads the snapshot and Bluetooth state, then redraws every placed widget. Debounced. */
    fun refresh(context: Context) {
        val app = context.applicationContext
        pending?.cancel()
        pending = scope.launch {
            delay(DEBOUNCE_MS)
            _state.value = load(app)
            push(app)
        }
    }

    suspend fun load(context: Context): WidgetData {
        if (demo) return _state.value
        val prefs = AppPrefs.get(context)
        val snapshot = prefs.loadHeadset()
        val device = connectedDevice(context)
        val live = device?.takeIf { snapshot == null || it.address == snapshot.address }
        return WidgetData(
            connected = live != null,
            battery = snapshot?.battery ?: live?.let(::systemBattery),
            charging = snapshot?.charging == true,
            mode = snapshot?.sound?.mode,
            name = snapshot?.name?.ifEmpty { null } ?: WidgetData().name,
            phone = Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME) ?: Build.MODEL,
            lowThreshold = prefs.alerts.value.threshold,
        ).also { _state.value = it; loaded = true }
    }

    private fun push(context: Context) {
        scope.launch { ALL_WIDGETS.forEach { runCatching { it.updateAll(context) } } }
    }

    private suspend fun connectedDevice(context: Context): BluetoothDevice? {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return null
        return runCatching { SonyDevices(context).connected() }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun systemBattery(device: BluetoothDevice): Int? = BatteryAlerts.systemBatteryLevel(device)
}
