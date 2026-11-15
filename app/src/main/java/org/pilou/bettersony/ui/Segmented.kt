package org.pilou.bettersony.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

private val TRACK_RADIUS = 11.dp
private val PILL_RADIUS = 9.dp
private val INSET = 3.dp
private val HEIGHT = 40.dp
private const val PRESSED_SCALE = 0.96f
private val PILL_SHADOW = Shadow(radius = 6.dp, color = Color(0x1F000000), offset = DpOffset(0.dp, 1.dp))

/**
 * iOS-style segmented control: one white pill glides between options on a spring, labels
 * cross-fade weight/colour, and the pressed option dips slightly. No ripples, no borders.
 */
@Composable
fun <T> Segmented(options: List<Pair<T, String>>, selected: T?, onSelect: (T) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val index = options.indexOfFirst { it.first == selected }
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(HEIGHT)
            .clip(RoundedCornerShape(TRACK_RADIUS))
            .background(Palette.segmentTrack)
            .padding(INSET),
    ) {
        val segment = maxWidth / options.size
        val pillX by animateDpAsState(
            segment * index.coerceAtLeast(0),
            spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
            label = "pill",
        )
        if (index >= 0) {
            Box(
                Modifier
                    .offset(x = pillX)
                    .width(segment)
                    .fillMaxHeight()
                    .dropShadow(RoundedCornerShape(PILL_RADIUS), PILL_SHADOW)
                    .background(Color.White, RoundedCornerShape(PILL_RADIUS)),
            )
        }
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, (key, label) ->
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(if (pressed) PRESSED_SCALE else 1f, label = "press")
                val on = i == index
                val color by animateColorAsState(if (on) Palette.ink else Palette.body, label = "label")
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clickable(interaction, indication = null, role = Role.Tab) {
                            if (!on) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onSelect(key)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        label,
                        style = (if (on) Type.bodyStrong else Type.body).copy(fontSize = Type.callout.fontSize, color = color),
                    )
                }
            }
        }
    }
}
