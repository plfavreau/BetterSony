package org.pilou.bettersony.alerts

import android.Manifest
import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.pilou.bettersony.data.AppPrefs
import org.pilou.bettersony.data.Headset
import org.pilou.bettersony.data.apply
import org.pilou.bettersony.data.SonyDevices
import org.pilou.bettersony.data.HeadsetSession
import org.pilou.bettersony.widget.WidgetStore

/**
 * Wakes up every ~15 minutes (feeds the low-battery alert and the widgets). If the headset is connected it reads the
 * battery (from the system when it knows it, otherwise with a quick MDR query) and alerts.
 */
class BatteryCheckJob : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var work: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        Log.i(TAG, "battery job started")
        work = scope.launch {
            runCatching { check(applicationContext) }.onFailure { Log.w(TAG, "battery check failed", it) }
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        work?.cancel()
        return true
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "BetterSony"
        private const val JOB_ID = 42
        private const val INTERVAL_MS = 15 * 60 * 1000L

        /** Set while the app holds its own connection, so the job doesn't open a second one. */
        @Volatile var appConnected = false

        fun schedule(context: Context) {
            val js = context.getSystemService(JobScheduler::class.java)
            if (js.getPendingJob(JOB_ID) != null) return
            js.schedule(
                JobInfo.Builder(JOB_ID, ComponentName(context, BatteryCheckJob::class.java))
                    .setPeriodic(INTERVAL_MS)
                    .setPersisted(true)
                    .build(),
            )
        }

        @SuppressLint("MissingPermission")
        suspend fun check(context: Context) {
            WidgetStore.refresh(context)
            fun skip(why: String) { Log.i(TAG, "background battery check skipped: $why") }
            if (appConnected) return skip("app holds the link")
            if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return skip("no bluetooth permission")
            }
            val device = SonyDevices(context).connected() ?: return skip("headset not connected")
            val level = BatteryAlerts.systemBatteryLevel(device) ?: readOverMdr(device) ?: return skip("no battery reading")
            Log.i(TAG, "background battery check: $level%")
            val prefs = AppPrefs.get(context)
            prefs.loadHeadset()?.takeIf { it.address == device.address }?.let { prefs.saveHeadset(it.copy(battery = level)) }
            BatteryAlerts.onLevel(context, level)
        }

        private suspend fun readOverMdr(device: BluetoothDevice): Int? {
            val session = HeadsetSession(device, Dispatchers.IO) { }
            return try {
                session.open()
                session.refreshBattery()?.let { Headset().apply(it).battery }
            } finally {
                session.close()
            }
        }
    }
}
