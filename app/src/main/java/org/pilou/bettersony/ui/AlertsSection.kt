package org.pilou.bettersony.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.pilou.bettersony.data.AlertSettings

@Composable
fun AlertsSection(
    alerts: AlertSettings,
    canNotify: Boolean,
    onChange: (AlertSettings) -> Unit,
    onAllowNotifications: () -> Unit,
) {
    SectionLabel("Notifications")
    Card(verticalPadding = 4.dp) {
        Column {
            SettingsRow("Low battery alert", divider = false) {
                Toggle(alerts.lowBattery, { on ->
                    onChange(alerts.copy(lowBattery = on))
                    if (on && !canNotify) onAllowNotifications()
                })
            }
            AnimatedVisibility(
                alerts.lowBattery,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(Modifier.padding(bottom = 14.dp)) {
                    SettingsRow("Alert me at") {}
                    Segmented(
                        options = AlertSettings.THRESHOLDS.map { it to "$it%" },
                        selected = alerts.threshold,
                        onSelect = { onChange(alerts.copy(threshold = it)) },
                    )
                }
            }
            if (alerts.lowBattery && !canNotify) {
                SettingsRow("Notifications are blocked", onClick = onAllowNotifications) {
                    BasicText("Allow", style = Type.bodyStrong.copy(color = Palette.blue))
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    BasicText(
        "Checked every 15 minutes while your headphones are connected, even when the app is closed.",
        style = Type.caption.copy(color = Palette.secondary),
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}
