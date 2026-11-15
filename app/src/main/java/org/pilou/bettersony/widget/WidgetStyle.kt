package org.pilou.bettersony.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import org.pilou.bettersony.MainActivity
import org.pilou.bettersony.R
import org.pilou.bettersony.data.ListenMode

/** "Pure" in light mode, solid graphite in dark mode (widgets cannot blur, so no glass). */
object WColors {
    val card = ColorProvider(day = Color.White, night = Color(0xFF1C1C1E))
    val text = ColorProvider(day = Color(0xFF111111), night = Color.White)
    val sub = ColorProvider(day = Color(0xFF6E6E73), night = Color(0xFFA1A1A6))
    val chip = ColorProvider(day = Color(0xFFF2F2F5), night = Color(0x1FFFFFFF))
    val chipOn = ColorProvider(day = Color(0xFF111111), night = Color.White)
    val chipOnText = ColorProvider(day = Color.White, night = Color(0xFF111111))
    val track = ColorProvider(day = Color(0xFFE9E9EE), night = Color(0x26FFFFFF))
    val green = ColorProvider(GREEN)
    val orange = ColorProvider(ORANGE)
    val idle = ColorProvider(Color(0xFFAEAEB2))
}

val GREEN = Color(0xFF34C759)
val ORANGE = Color(0xFFFF9500)
val CARD_RADIUS = 24.dp
private const val RING_PX = 240
private const val RING_STROKE_PX = 16f
private const val TRACK_ARGB = 0x408E8E93

fun levelColor(d: WidgetData): ColorProvider = if (d.low) WColors.orange else WColors.green

fun openApp(): Action = actionStartActivity<MainActivity>()

fun GlanceModifier.card(d: WidgetData): GlanceModifier =
    fillMaxSize().cornerRadius(CARD_RADIUS).background(WColors.card).clickable(openApp())

fun style(size: TextUnit, color: ColorProvider = WColors.text, bold: Boolean = true) =
    TextStyle(color = color, fontSize = size, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)

fun pctText(d: WidgetData): String = d.battery?.let { "$it%" } ?: "--"

fun statusText(d: WidgetData): String = when {
    !d.connected -> "Not connected"
    d.turningOff -> "Turning off…"
    d.charging -> "Charging"
    else -> d.mode?.let(::modeShort) ?: "Connected"
}

/** A battery ring drawn into a bitmap (RemoteViews can't draw arcs). Grey track reads on light and dark. */
fun ringBitmap(level: Int?, low: Boolean, dim: Boolean): Bitmap {
    val bmp = Bitmap.createBitmap(RING_PX, RING_PX, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val inset = RING_STROKE_PX / 2 + 1
    val rect = RectF(inset, inset, RING_PX - inset, RING_PX - inset)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = RING_STROKE_PX; strokeCap = Paint.Cap.ROUND }
    paint.color = TRACK_ARGB
    c.drawArc(rect, 0f, 360f, false, paint)
    if (level != null && level > 0) {
        paint.color = if (dim) Color(0xFFAEAEB2).toArgb() else if (low) ORANGE.toArgb() else GREEN.toArgb()
        c.drawArc(rect, -90f, 360f * level / 100f, false, paint)
    }
    return bmp
}

@Composable
fun Ring(d: WidgetData, modifier: GlanceModifier) {
    Image(ImageProvider(ringBitmap(d.battery, d.low, !d.connected)), contentDescription = null, modifier = modifier)
}

@Composable
fun BatteryBar(d: WidgetData, modifier: GlanceModifier = GlanceModifier) {
    LinearProgressIndicator(
        progress = (d.battery ?: 0) / 100f,
        modifier = modifier.height(6.dp).cornerRadius(3.dp),
        color = if (d.connected) levelColor(d) else WColors.idle,
        backgroundColor = WColors.track,
    )
}

/** One mode button. Selected = inverted pill. Disabled (headphones away) just opens the app. */
@Composable
fun ModeButton(d: WidgetData, mode: ListenMode, modifier: GlanceModifier, label: Boolean, iconSize: Dp = 20.dp, radius: Dp = 14.dp) {
    val on = d.connected && d.mode == mode
    val tint = if (on) WColors.chipOnText else if (d.connected) WColors.text else WColors.sub
    val action = if (d.connected) {
        actionRunCallback<SetModeAction>(actionParametersOf(SetModeAction.MODE_KEY to mode.name))
    } else openApp()
    Box(
        modifier = modifier.cornerRadius(radius).background(if (on) WColors.chipOn else WColors.chip).clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(ImageProvider(modeIcon(mode)), mode.label, GlanceModifier.size(iconSize), colorFilter = ColorFilter.tint(tint))
            if (label) Text(modeShort(mode), style = style(11.sp, tint), maxLines = 1, modifier = GlanceModifier.padding(top = 2.dp))
        }
    }
}

/** Turns the headphones off. Shown as a plain chip; dimmed while they are away or already turning off. */
@Composable
fun PowerButton(d: WidgetData, modifier: GlanceModifier, label: Boolean = false, iconSize: Dp = 20.dp, radius: Dp = 14.dp) {
    val active = d.connected && !d.turningOff
    val tint = if (active) WColors.text else WColors.sub
    Box(
        modifier = modifier.cornerRadius(radius).background(WColors.chip)
            .clickable(if (active) actionRunCallback<PowerOffAction>() else openApp()),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(ImageProvider(R.drawable.wm_power), "Turn off", GlanceModifier.size(iconSize), colorFilter = ColorFilter.tint(tint))
            if (label) Text("Power", style = style(11.sp, tint), maxLines = 1, modifier = GlanceModifier.padding(top = 2.dp))
        }
    }
}
