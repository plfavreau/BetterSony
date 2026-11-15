package org.pilou.bettersony.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val THUMB = 26.dp
private val TRACK = 6.dp

@Composable
fun Icon(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Image(rememberVectorPainter(icon), null, modifier.size(size), colorFilter = ColorFilter.tint(tint))
}

@Composable
fun Card(modifier: Modifier = Modifier, verticalPadding: Dp = 16.dp, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
            .background(Palette.surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = verticalPadding),
    ) { content() }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText(text, style = Type.label)
        trailing()
    }
}

/** Discrete iOS-style slider: black fill, white floating thumb, a tick of haptics per step. */
@Composable
fun StepSlider(
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    onCommit: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    origin: Int = range.first,
) {
    val haptics = LocalHapticFeedback.current
    val latest by rememberUpdatedState(value)
    val steps = (range.last - range.first).toFloat()
    BoxWithConstraints(modifier.fillMaxWidth().height(THUMB + 8.dp)) {
        val density = LocalDensity.current
        val thumbPx = with(density) { THUMB.toPx() }
        val widthPx = constraints.maxWidth - thumbPx
        var dragX by remember { mutableFloatStateOf(0f) }
        fun valueAt(x: Float): Int = (range.first + ((x - thumbPx / 2) / widthPx).coerceIn(0f, 1f) * steps).roundToInt()
        fun emit(v: Int) {
            if (v != latest) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                onChange(v)
            }
        }
        val fraction = (value - range.first) / steps
        val thumbOffset by animateDpAsState(
            with(density) { (fraction * widthPx).toDp() },
            spring(stiffness = Spring.StiffnessHigh),
            label = "thumb",
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(THUMB + 8.dp)
                .pointerInput(enabled, range) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { emit(valueAt(it.x)); onCommit(valueAt(it.x)) }
                }
                .pointerInput(enabled, range) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { dragX = it.x; emit(valueAt(it.x)) },
                        onDragEnd = { onCommit(latest) },
                        onHorizontalDrag = { _, dx -> dragX += dx; emit(valueAt(dragX)) },
                    )
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            val trackColor = if (enabled) Palette.ink else Palette.tertiary
            Box(Modifier.padding(horizontal = THUMB / 2).fillMaxWidth().height(TRACK).clip(CircleShape).background(Palette.track))
            val originOffset = with(density) { ((origin - range.first) / steps * widthPx).toDp() }
            val fillStart = minOf(originOffset, thumbOffset)
            val fillWidth = maxOf(originOffset, thumbOffset) - fillStart
            Box(
                Modifier.padding(start = THUMB / 2).offset(x = fillStart).width(fillWidth).height(TRACK)
                    .clip(CircleShape).background(trackColor),
            )
            Box(
                Modifier
                    .offset(x = thumbOffset)
                    .size(THUMB)
                    .shadow(4.dp, CircleShape)
                    .background(Color.White, CircleShape),
            )
        }
    }
}

@Composable
fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val track by animateColorAsState(if (checked) Palette.green else Palette.track, label = "track")
    val knob by animateDpAsState(if (checked) 20.dp else 0.dp, spring(dampingRatio = 0.7f), label = "knob")
    Box(
        Modifier
            .width(51.dp)
            .height(31.dp)
            .clip(CircleShape)
            .background(track)
            .clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }
            .padding(2.dp),
    ) {
        Box(Modifier.offset(x = knob).size(27.dp).shadow(2.dp, CircleShape).background(Color.White, CircleShape))
    }
}

@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    divider: Boolean = true,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    Column(modifier) {
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.divider))
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                BasicText(title, style = Type.body)
                subtitle?.let { BasicText(it, style = Type.caption.copy(color = Palette.secondary), modifier = Modifier.padding(top = 2.dp)) }
            }
            trailing()
        }
    }
}

@Composable
fun PillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(Palette.ink)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 14.dp),
    ) { BasicText(text, style = Type.button) }
}

@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
