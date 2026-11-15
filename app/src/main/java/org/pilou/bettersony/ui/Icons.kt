package org.pilou.bettersony.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import org.pilou.bettersony.data.ListenMode

private const val VIEWPORT = 24f
private const val STROKE = 1.8f

private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"

private fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float) =
    "M${cx - rx},$cy a$rx,$ry 0 1,0 ${2 * rx},0 a$rx,$ry 0 1,0 ${-2 * rx},0"

/** Builds a 24x24 outline icon (tinted by the caller) from SVG path strings. */
private fun outline(name: String, vararg paths: String, filled: List<String> = emptyList()): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, VIEWPORT, VIEWPORT).apply {
        paths.forEach {
            addPath(
                addPathNodes(it), stroke = SolidColor(Color.Black), strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            )
        }
        filled.forEach { addPath(addPathNodes(it), fill = SolidColor(Color.Black)) }
    }.build()

object Icons {
    val noiseCancelling = outline("nc", "M12 3a9 9 0 1 0 9 9", "M12 7a5 5 0 1 0 5 5", filled = listOf(circle(12f, 12f, 1.2f)))
    val wind = outline("wind", "M3 8h10a3 3 0 1 0-3-3", "M3 12h15a3 3 0 1 1-3 3", "M3 16h7")
    val ambient = outline("ambient", "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z", circle(12f, 12f, 3f))
    val off = outline("off", circle(12f, 12f, 9f), "M5.6 18.4L18.4 5.6")
    val power = outline("power", "M12 3v8", "M6.3 6.8a8 8 0 1 0 11.4 0")
    val talk = outline("talk", "M4 5h16a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H9l-5 4V6a1 1 0 0 1 1-1z", "M8 10h8", "M8 13h5")
    val chevron = outline("chevron", "M9 6l6 6-6 6")
    val close = outline("close", "M6 6l12 12", "M18 6L6 18")
    val bolt = outline("bolt", "M13 2L4 14h7l-1 8 9-12h-7l1-8z")
    val bluetooth = outline("bluetooth", "M7 7l10 10-5 5V2l5 5L7 17")
    val gear = outline(
        "gear", circle(12f, 12f, 3f),
        "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21" +
            "a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1" +
            "a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8" +
            "l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1" +
            "a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9" +
            "a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z",
    )

    fun forMode(mode: ListenMode): ImageVector = when (mode) {
        ListenMode.NOISE_CANCELLING -> noiseCancelling
        ListenMode.WIND -> wind
        ListenMode.AMBIENT -> ambient
        ListenMode.OFF -> off
    }
}
