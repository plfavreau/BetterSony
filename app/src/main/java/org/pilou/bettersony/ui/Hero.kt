package org.pilou.bettersony.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.pilou.bettersony.R

const val LOW_BATTERY = 20

@Composable
fun Header(name: String, phone: String, syncing: Boolean, restarting: Boolean, onPower: () -> Unit, onSettings: () -> Unit) {
    val dot by animateColorAsState(if (syncing) Palette.tertiary else Palette.green, label = "dot")
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(name, style = Type.largeTitle, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Box(Modifier.size(7.dp).background(dot, CircleShape))
                Spacer(Modifier.width(6.dp))
                BasicText(when { restarting -> "Restarting…"; syncing -> "Connecting…"; else -> "Connected · $phone" }, style = Type.callout, maxLines = 1)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundButton(Icons.power, "Turn off", onPower)
            RoundButton(Icons.gear, "Settings", onSettings)
        }
    }
}

@Composable
private fun RoundButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .shadow(1.dp, CircleShape)
            .background(Color.White, CircleShape)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, Palette.ink, size = 18.dp) }
}

@Composable
fun HeadphoneImage(modifier: Modifier = Modifier, alpha: Float = 1f) {
    Image(
        painterResource(R.drawable.hp_black),
        contentDescription = "Headphones",
        modifier = modifier,
        alpha = alpha,
    )
}

@Composable
fun Hero(battery: Int?, charging: Boolean, codec: String?) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(270.dp), contentAlignment = Alignment.Center) {
            HeadphoneImage(Modifier.height(230.dp))
            if (battery != null) {
                BatteryPill(battery, charging, Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
            }
        }
        codec?.let { BasicText(it, style = Type.caption, modifier = Modifier.padding(top = 10.dp)) }
    }
}

@Composable
private fun BatteryPill(percent: Int, charging: Boolean, modifier: Modifier) {
    val fill = if (percent <= LOW_BATTERY && !charging) Palette.orange else Palette.green
    Row(
        modifier
            .shadow(6.dp, CircleShape, ambientColor = Color(0x10000000), spotColor = Color(0x18000000))
            .background(Color.White, CircleShape)
            .padding(start = 10.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(26.dp)
                    .height(13.dp)
                    .border(1.6.dp, Palette.batteryOutline, RoundedCornerShape(4.dp))
                    .padding(2.5.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(percent.coerceIn(4, 100) / 100f)
                        .fillMaxHeight()
                        .background(fill, RoundedCornerShape(2.dp)),
                )
            }
            Box(Modifier.padding(start = 1.dp).width(2.dp).height(5.dp).background(Palette.batteryOutline, RoundedCornerShape(1.dp)))
        }
        Spacer(Modifier.width(8.dp))
        BasicText(
            if (charging) "$percent% · Charging" else "$percent%",
            style = Type.bodyStrong.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

private const val RATED_HOURS = 30
private const val PERCENT = 100

/** Quiet card in the same language as the rest of the screen: tinted glyph tile, title, one line of context. */
@Composable
fun LowBatteryNotice(percent: Int) {
    val hours = percent * RATED_HOURS / PERCENT
    val detail = if (hours < 1) "Under an hour of listening left" else "About ${hours}h of listening left"
    Card(Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp), verticalPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(Palette.orangeTint, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { BatteryGlyph(percent, Palette.orange) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                BasicText("Charge soon", style = Type.bodyStrong)
                BasicText(detail, style = Type.callout.copy(fontSize = 13.sp))
            }
            BasicText("$percent%", style = Type.bodyStrong.copy(color = Palette.orange))
        }
    }
}

@Composable
private fun BatteryGlyph(percent: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.width(20.dp).height(11.dp).border(1.5.dp, color, RoundedCornerShape(3.5.dp)).padding(2.dp),
        ) {
            Box(Modifier.fillMaxWidth(percent.coerceIn(10, 100) / 100f).fillMaxHeight().background(color, RoundedCornerShape(1.5.dp)))
        }
        Box(Modifier.padding(start = 1.dp).width(1.5.dp).height(4.dp).background(color, RoundedCornerShape(1.dp)))
    }
}
