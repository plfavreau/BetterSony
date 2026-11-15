package org.pilou.bettersony.alerts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.Notification
import android.bluetooth.BluetoothDevice
import org.pilou.bettersony.MainActivity
import org.pilou.bettersony.R
import org.pilou.bettersony.data.AppPrefs

/**
 * Low-battery notifications. Fires at your threshold, again at 10% and 5%, and once more each time
 * the headphones reconnect while still low. Re-armed (and cleared) once they're charged again.
 */
object BatteryAlerts {
    private const val CHANNEL = "battery"
    private const val NOTIFICATION_ID = 1
    private const val REARM_MARGIN = 5
    private const val RATED_HOURS = 30
    private const val PERCENT = 100
    private const val DEFAULT_NAME = "Headphones"

    private val ESCALATION = listOf(10, 5)

    fun onLevel(context: Context, level: Int) {
        val name = AppPrefs.get(context).loadHeadset()?.name?.ifEmpty { null } ?: DEFAULT_NAME
        val prefs = AppPrefs.get(context)
        val settings = prefs.alerts.value
        if (level > settings.threshold + REARM_MARGIN) {
            if (prefs.lowAlertStep != 0) {
                prefs.lowAlertStep = 0
                context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
            }
            return
        }
        if (!settings.lowBattery || level > settings.threshold) return
        val steps = (listOf(settings.threshold) + ESCALATION).filter { it <= settings.threshold }.distinct()
        val step = steps.filter { level <= it }.min()
        val last = prefs.lowAlertStep
        if (last != 0 && step >= last) return
        if (post(context, name, level)) prefs.lowAlertStep = step
    }

    /** The headphones just came back: remind again on the next reading if they're still low. */
    fun rearm(context: Context) {
        AppPrefs.get(context).lowAlertStep = 0
    }

    /** Android tracks headset battery (HFP/AVRCP) but only exposes it through a hidden getter. */
    fun systemBatteryLevel(device: BluetoothDevice): Int? = runCatching {
        device.javaClass.getMethod("getBatteryLevel").invoke(device) as Int
    }.getOrNull()?.takeIf { it in 0..PERCENT }

    fun canNotify(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun post(context: Context, name: String, level: Int): Boolean {
        if (!canNotify(context)) return false
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Low battery", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Tells you when your headphones need charging"
            },
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val hours = (level * RATED_HOURS / PERCENT).coerceAtLeast(1)
        val body = if (hours <= 1) "Less than an hour of listening left. Time to charge."
        else "About $hours hours of listening left. Time to charge."
        val n = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("$name at $level%")
            .setContentText(body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .build()
        nm.notify(NOTIFICATION_ID, n)
        return true
    }
}
