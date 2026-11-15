package org.pilou.bettersony.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pilou.bettersony.alerts.BatteryAlerts

/** Keeps the link open a while after the last use, so reopening the app or tapping a widget is instant. */
private const val GRACE_MS = 90_000L
private const val SYNC_TIMEOUT_MS = 45_000L
private const val RECHECK_MS = 15_000L

/**
 * One headset link for the whole process, shared by the app and the widgets.
 * The app holds it while visible; widget taps borrow it; it closes [GRACE_MS] after the last use.
 */
object Headsets {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Volatile private var repo: HeadsetRepository? = null
    private var foreground = false
    private var stopJob: Job? = null

    fun repo(context: Context): HeadsetRepository = repo ?: synchronized(this) {
        repo ?: context.applicationContext.let { app ->
            HeadsetRepository(SonyDevices(app), AppPrefs.get(app), scope) { BatteryAlerts.onLevel(app, it) }
        }.also { repo = it }
    }

    fun onForeground(context: Context) {
        foreground = true
        stopJob?.cancel()
        repo(context).start()
    }

    fun onBackground(context: Context) {
        foreground = false
        scheduleStop(context)
    }

    /** Widget or tile action: reuse the open link, or open one and keep it for the grace period. */
    private suspend fun borrow(context: Context, action: suspend (HeadsetRepository) -> Boolean): Boolean =
        withContext(Dispatchers.Main.immediate) {
            val r = repo(context)
            r.start()
            if (!foreground) scheduleStop(context)
            if (!r.ensureLinked()) return@withContext false
            action(r)
        }

    suspend fun setModeFromWidget(context: Context, mode: ListenMode): Boolean = borrow(context) { r ->
        val current = r.snapshot()?.sound ?: SoundControl()
        r.setSound(current.copy(mode = mode))
        true
    }

    /**
     * Headphones just connected: open the link (the repository restores speak-to-chat), then check
     * again a little later, since the official app may push its own settings after connecting.
     */
    suspend fun onHeadsetConnected(context: Context) {
        val r = repo(context)
        borrow(context) { true }
        withTimeoutOrNull(SYNC_TIMEOUT_MS) { r.state.first { it is ConnectionState.Connected && !it.syncing } } ?: return
        delay(RECHECK_MS)
        r.recheckTalk()
    }

    suspend fun powerOff(context: Context): Boolean = borrow(context) { it.powerOff() }

    private fun scheduleStop(context: Context) {
        stopJob?.cancel()
        stopJob = scope.launch {
            delay(GRACE_MS)
            if (!foreground) repo(context).stop()
        }
    }
}
