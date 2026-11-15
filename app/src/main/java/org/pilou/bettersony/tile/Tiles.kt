package org.pilou.bettersony.tile

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.pilou.bettersony.R
import org.pilou.bettersony.data.HeadsetJob
import org.pilou.bettersony.data.Headsets
import org.pilou.bettersony.data.ListenMode
import org.pilou.bettersony.widget.QUICK_MODES
import org.pilou.bettersony.widget.WidgetData
import org.pilou.bettersony.widget.WidgetStore
import org.pilou.bettersony.widget.modeIcon

/** Shared plumbing: both tiles render from [WidgetStore], the same snapshot the widgets use. */
abstract class HeadsetTile : TileService() {
    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watch: Job? = null

    override fun onStartListening() {
        watch?.cancel()
        watch = scope.launch {
            WidgetStore.load(applicationContext)
            WidgetStore.state.collect { render(it) }
        }
    }

    override fun onStopListening() {
        watch?.cancel()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun render(d: WidgetData) {
        val tile = qsTile ?: return
        draw(tile, d)
        tile.updateTile()
    }

    protected abstract fun draw(tile: Tile, d: WidgetData)
}

/** Turns the headphones off, the one-tap alternative to turning Bluetooth off. */
class PowerTile : HeadsetTile() {
    override fun draw(tile: Tile, d: WidgetData) {
        tile.label = "Headphones"
        tile.subtitle = when {
            d.turningOff -> "Turning off"
            d.connected -> d.battery?.let { "$it% · Tap to turn off" } ?: "Tap to turn off"
            else -> "Not connected"
        }
        tile.state = if (d.connected && !d.turningOff) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, R.drawable.wm_power)
    }

    override fun onClick() {
        val d = WidgetStore.state.value
        if (!d.connected || d.turningOff) return
        WidgetStore.patch(applicationContext) { it.copy(turningOff = true) }
        HeadsetJob.schedule(applicationContext, HeadsetJob.Task.POWER_OFF)
    }
}

/** Cycles noise cancelling, ambient sound and off. */
class NoiseTile : HeadsetTile() {
    override fun draw(tile: Tile, d: WidgetData) {
        val mode = d.mode ?: ListenMode.NOISE_CANCELLING
        tile.label = mode.label
        tile.subtitle = if (d.connected) "Tap to switch" else "Not connected"
        tile.state = if (!d.connected) Tile.STATE_UNAVAILABLE else if (mode == ListenMode.OFF) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        tile.icon = Icon.createWithResource(this, modeIcon(mode))
    }

    override fun onClick() {
        val d = WidgetStore.state.value
        if (!d.connected) return
        val next = QUICK_MODES[(QUICK_MODES.indexOf(d.mode) + 1) % QUICK_MODES.size]
        WidgetStore.patch(applicationContext) { it.copy(mode = next) }
        scope.launch {
            val ok = runCatching { Headsets.setModeFromWidget(applicationContext, next) }.getOrDefault(false)
            if (!ok) WidgetStore.patch(applicationContext) { it.copy(mode = d.mode) }
        }
    }
}
