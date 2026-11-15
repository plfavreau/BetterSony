package org.pilou.bettersony.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import org.pilou.bettersony.R


@Composable
private fun Headphones(w: Int, h: Int) {
    Image(ImageProvider(R.drawable.hp_black), "Headphones", GlanceModifier.size(w.dp, h.dp))
}

@Composable
private fun Dot(d: WidgetData) {
    Box(GlanceModifier.size(7.dp).cornerRadius(4.dp).background(if (d.connected) WColors.green else WColors.idle)) {}
}

/** A · battery ring around the headphones. */
@Composable
fun RingWidgetContent(d: WidgetData) {
    Box(GlanceModifier.card(d).padding(10.dp), contentAlignment = Alignment.Center) {
        Ring(d, GlanceModifier.fillMaxSize())
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Headphones(56, 46)
            Text(pctText(d), style = style(20.sp, if (d.low && d.connected) WColors.orange else WColors.text))
            Text(statusText(d), style = style(11.sp, WColors.sub), maxLines = 1)
        }
    }
}

/** B · big percentage, bar and current mode. */
@Composable
fun BigNumberWidgetContent(d: WidgetData) {
    Column(GlanceModifier.card(d).padding(16.dp)) {
        Text(d.name, style = style(12.sp, WColors.sub), maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(d.battery?.toString() ?: "--", style = style(40.sp, if (d.low && d.connected) WColors.orange else WColors.text))
            Text("%", style = style(20.sp), modifier = GlanceModifier.padding(bottom = 6.dp))
        }
        Spacer(GlanceModifier.defaultWeight())
        BatteryBar(d, GlanceModifier.fillMaxWidth())
        Spacer(GlanceModifier.height(10.dp))
        Row(
            GlanceModifier.fillMaxWidth().cornerRadius(14.dp).background(WColors.chip).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val mode = d.mode
            if (d.connected && mode != null) {
                Image(ImageProvider(modeIcon(mode)), null, GlanceModifier.size(14.dp), colorFilter = ColorFilter.tint(WColors.text))
            } else {
                Dot(d)
            }
            Text(if (d.connected) statusText(d) else "Offline", style = style(12.sp), maxLines = 1, modifier = GlanceModifier.padding(start = 6.dp))
        }
    }
}

/** C · battery, three mode buttons and the power button in one row. */
@Composable
fun ModeBarWidgetContent(d: WidgetData) {
    Row(GlanceModifier.card(d).padding(start = 16.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(GlanceModifier.width(58.dp)) {
            Text(pctText(d), style = style(19.sp, if (d.low && d.connected) WColors.orange else WColors.text))
            Text(if (d.connected) modeLine(d) else "Offline", style = style(11.sp, WColors.sub, bold = false), maxLines = 1)
        }
        Row(GlanceModifier.defaultWeight()) {
            QUICK_MODES.forEach { m ->
                ModeButton(d, m, GlanceModifier.defaultWeight().height(40.dp), label = false)
                Spacer(GlanceModifier.width(6.dp))
            }
            PowerButton(d, GlanceModifier.defaultWeight().height(40.dp))
        }
    }
}

/** D · headphones, status, battery and labelled mode buttons. */
@Composable
fun DashboardWidgetContent(d: WidgetData) {
    Column(GlanceModifier.card(d).padding(12.dp)) {
        Row(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
            Headphones(84, 70)
            Column(GlanceModifier.defaultWeight().padding(start = 10.dp)) {
                Text(d.name, style = style(17.sp), maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(d)
                    Text(
                        if (d.connected && !d.turningOff) "Connected · ${d.phone}" else statusText(d),
                        style = style(12.sp, WColors.sub, bold = false), maxLines = 1, modifier = GlanceModifier.padding(start = 6.dp),
                    )
                }
                Spacer(GlanceModifier.height(10.dp))
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    BatteryBar(d, GlanceModifier.defaultWeight())
                    Text(pctText(d), style = style(13.sp, if (d.low && d.connected) WColors.orange else WColors.text), modifier = GlanceModifier.padding(start = 8.dp))
                }
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            QUICK_MODES.forEach { m ->
                ModeButton(d, m, GlanceModifier.defaultWeight().height(50.dp), label = true, iconSize = 18.dp)
                Spacer(GlanceModifier.width(6.dp))
            }
            PowerButton(d, GlanceModifier.defaultWeight().height(50.dp), label = true, iconSize = 18.dp)
        }
    }
}

/** E · small battery ring with the current mode icon. */
@Composable
fun MiniBatteryWidgetContent(d: WidgetData) {
    Row(GlanceModifier.card(d).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(GlanceModifier.size(44.dp), contentAlignment = Alignment.Center) {
            Ring(d, GlanceModifier.fillMaxSize())
            d.mode?.takeIf { d.connected }?.let {
                Image(ImageProvider(modeIcon(it)), null, GlanceModifier.size(18.dp), colorFilter = ColorFilter.tint(WColors.text))
            }
        }
        Column(GlanceModifier.padding(start = 10.dp)) {
            Text(pctText(d), style = style(18.sp, if (d.low && d.connected) WColors.orange else WColors.text))
            Text(statusText(d), style = style(12.sp, WColors.sub, bold = false), maxLines = 1)
        }
    }
}

/** E · three round quick-mode buttons. */
@Composable
fun MiniModesWidgetContent(d: WidgetData) {
    Row(GlanceModifier.card(d).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        QUICK_MODES.forEachIndexed { i, m ->
            if (i > 0) Spacer(GlanceModifier.width(4.dp))
            ModeButton(d, m, GlanceModifier.defaultWeight().height(38.dp), label = false, iconSize = 20.dp, radius = 19.dp)
        }
    }
}

/** F · one big power button, to turn the headphones off without touching Bluetooth. */
@Composable
fun PowerWidgetContent(d: WidgetData) {
    val active = d.connected && !d.turningOff
    Column(
        GlanceModifier.card(d).padding(10.dp).clickable(if (active) actionRunCallback<PowerOffAction>() else openApp()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            GlanceModifier.size(46.dp).cornerRadius(23.dp).background(if (active) WColors.chipOn else WColors.chip),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                ImageProvider(R.drawable.wm_power), "Turn off", GlanceModifier.size(24.dp),
                colorFilter = ColorFilter.tint(if (active) WColors.chipOnText else WColors.sub),
            )
        }
        Spacer(GlanceModifier.height(6.dp))
        Text(
            when {
                d.turningOff -> "Turning off"
                d.connected -> "Turn off"
                else -> "Off"
            },
            style = style(12.sp, if (active) WColors.text else WColors.sub), maxLines = 1,
        )
    }
}

private fun modeLine(d: WidgetData): String = if (d.turningOff) "Turning off" else d.mode?.let(::modeShort) ?: d.name

