package org.pilou.bettersony

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import org.pilou.bettersony.alerts.BatteryAlerts
import org.pilou.bettersony.alerts.BatteryCheckJob
import org.pilou.bettersony.data.ListenMode
import org.pilou.bettersony.ui.App
import org.pilou.bettersony.ui.HomeViewModel
import org.pilou.bettersony.widget.WidgetStore

class MainActivity : ComponentActivity() {
    private val vm: HomeViewModel by viewModels()
    private var askedOnce = false

    private val permission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.onForeground(hasPermission())
    }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App(
                vm = vm,
                onGrantPermission = ::requestPermission,
                onRequestNotifications = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) },
                onOpenBluetooth = { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (BuildConfig.DEBUG) debugWidgets()
    }

    override fun onStart() {
        super.onStart()
        if (BuildConfig.DEBUG) debugWidgets()
        if (BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_DEMO, false)) {
            vm.repo.showDemo(intent.getIntExtra(EXTRA_DEMO_BATTERY, DEMO_BATTERY))
            return
        }
        vm.onForeground(hasPermission())
        BatteryCheckJob.schedule(this)
        val needsNotifications = !BatteryAlerts.canNotify(this) && !vm.prefs.askedNotifications
        if ((!hasPermission() || needsNotifications) && !askedOnce) {
            askedOnce = true
            vm.prefs.askedNotifications = true
            requestPermission()
        }
    }

    /** Debug: `--es pin RingWidgetReceiver` asks the launcher to place a widget; `--ez widgetDemo true` fakes data. */
    private fun debugWidgets() {
        intent.getStringExtra(EXTRA_PIN)?.let {
            getSystemService(AppWidgetManager::class.java)
                .requestPinAppWidget(ComponentName(this, "$packageName.widget.$it"), null, null)
            intent.removeExtra(EXTRA_PIN)
        }
        if (intent.getBooleanExtra(EXTRA_WIDGET_DEMO, false)) {
            WidgetStore.demo = true
            WidgetStore.patch(this) {
                it.copy(connected = true, battery = intent.getIntExtra(EXTRA_DEMO_BATTERY, DEMO_BATTERY), mode = ListenMode.NOISE_CANCELLING, phone = "Pixel")
            }
        }
    }

    override fun onStop() {
        super.onStop()
        vm.onBackground()
    }

    private fun hasPermission(): Boolean =
        checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission() =
        permission.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.POST_NOTIFICATIONS))

    companion object {
        private const val EXTRA_DEMO = "demo"
        private const val EXTRA_DEMO_BATTERY = "battery"
        private const val DEMO_BATTERY = 72
        private const val EXTRA_PIN = "pin"
        private const val EXTRA_WIDGET_DEMO = "widgetDemo"
    }
}
