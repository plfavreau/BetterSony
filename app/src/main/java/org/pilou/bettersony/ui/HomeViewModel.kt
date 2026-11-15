package org.pilou.bettersony.ui

import android.app.Application
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.pilou.bettersony.alerts.BatteryAlerts
import org.pilou.bettersony.alerts.BatteryCheckJob
import org.pilou.bettersony.data.AlertSettings
import org.pilou.bettersony.data.AppPrefs
import org.pilou.bettersony.data.HeadsetRepository
import org.pilou.bettersony.data.Headsets
import org.pilou.bettersony.data.RestartChange
import org.pilou.bettersony.data.apply

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = AppPrefs.get(app)
    val repo: HeadsetRepository = Headsets.repo(app)
    val alerts = prefs.alerts
    val keepTalk = prefs.keepTalk

    /** Turning the guard back on also restores your choice right away. */
    fun setKeepTalk(on: Boolean) {
        prefs.setKeepTalk(on)
        if (on) viewModelScope.launch { repo.recheckTalk() }
    }

    fun powerOff() {
        viewModelScope.launch { repo.powerOff() }
    }

    fun restart(change: RestartChange) {
        viewModelScope.launch { repo.apply(change) }
    }

    fun setAlerts(settings: AlertSettings) {
        prefs.setAlerts(settings)
        BatteryCheckJob.schedule(getApplication())
        repo.currentBattery()?.let { BatteryAlerts.onLevel(getApplication(), it) }
    }
    val state = repo.state
    val phoneName: String =
        Settings.Global.getString(app.contentResolver, Settings.Global.DEVICE_NAME) ?: Build.MODEL

    fun onForeground(hasPermission: Boolean) {
        if (hasPermission) Headsets.onForeground(getApplication()) else repo.requirePermission()
    }

    /** The link outlives the screen: [Headsets] closes it after a grace period. */
    fun onBackground() = Headsets.onBackground(getApplication())
}
