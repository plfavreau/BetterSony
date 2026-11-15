package org.pilou.bettersony.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.pilou.bettersony.data.ConnectionState
import org.pilou.bettersony.data.RestartChange
import org.pilou.bettersony.alerts.BatteryAlerts
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

@Composable
fun App(
    vm: HomeViewModel,
    onGrantPermission: () -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenBluetooth: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val alerts by vm.alerts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val resumed by lifecycle.currentStateAsState()
    val canNotify = remember(resumed, showSettingsKey(state)) { BatteryAlerts.canNotify(context) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var confirmOff by rememberSaveable { mutableStateOf(false) }
    var pendingRestart by remember { mutableStateOf<RestartChange?>(null) }
    val keepTalk by vm.keepTalk.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(Palette.background)) {
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen",
        ) { st ->
            when (st) {
                is ConnectionState.Connected ->
                    HomeContent(
                        st.headset, st.syncing, st.restarting, vm.phoneName, vm.repo, keepTalk, vm::setKeepTalk,
                        onPower = { confirmOff = true },
                        onSettings = { showSettings = true },
                        onRestart = { pendingRestart = it },
                    )
                is ConnectionState.Connecting -> ConnectingScreen(st.name)
                is ConnectionState.Disconnected -> EmptyScreen(
                    title = if (st.canConnect) "Headphones not connected" else "No headphones paired",
                    message = st.error ?: if (st.canConnect) {
                        "Turn them on and they'll show up here on their own. No need to pair again."
                    } else {
                        "Pair your WH-1000XM4 in Bluetooth settings once. After that, they show up here on their own."
                    },
                    action = if (st.canConnect) "Connect" else null,
                    onAction = vm.repo::connectManually,
                    secondary = "Bluetooth settings" to onOpenBluetooth,
                )
                ConnectionState.BluetoothOff -> EmptyScreen(
                    title = "Bluetooth is off",
                    message = "Turn on Bluetooth and your headphones will connect on their own.",
                    action = "Open Bluetooth",
                    onAction = onOpenBluetooth,
                )
                ConnectionState.NeedsPermission -> EmptyScreen(
                    title = "Allow nearby devices",
                    message = "This app talks to your headphones over Bluetooth. It needs the Nearby devices permission to do that.",
                    action = "Allow",
                    onAction = onGrantPermission,
                )
            }
        }
        val connected = state as? ConnectionState.Connected
        if (connected != null) {
            SettingsSheet(
                visible = showSettings,
                headset = connected.headset,
                alerts = alerts,
                canNotify = canNotify,
                onAlerts = vm::setAlerts,
                onAllowNotifications = onRequestNotifications,
                onDismiss = { showSettings = false },
            )
            PowerDialog(
                visible = confirmOff,
                name = connected.headset.name,
                onConfirm = { confirmOff = false; vm.powerOff() },
                onDismiss = { confirmOff = false },
            )
            RestartDialog(
                change = pendingRestart,
                onConfirm = { pendingRestart?.let(vm::restart); pendingRestart = null },
                onDismiss = { pendingRestart = null },
            )
        }
    }
}

private fun showSettingsKey(state: ConnectionState): Boolean = state is ConnectionState.Connected
