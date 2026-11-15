package org.pilou.bettersony.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val FLOAT_MS = 1300
private const val SPIN_MS = 800
private const val PULSE_MS = 700
private const val FULL_TURN = 360f
private const val SPINNER_SWEEP = 90f
private const val GREYED_ALPHA = 0.35f

@Composable
fun ConnectingScreen(name: String) {
    val t = rememberInfiniteTransition(label = "connecting")
    val lift by t.animateFloat(0f, -10f, infiniteRepeatable(tween(FLOAT_MS), RepeatMode.Reverse), label = "lift")
    val angle by t.animateFloat(0f, FULL_TURN, infiniteRepeatable(tween(SPIN_MS, easing = LinearEasing)), label = "spin")
    val pulse by t.animateFloat(1f, 0.55f, infiniteRepeatable(tween(PULSE_MS), RepeatMode.Reverse), label = "pulse")
    Column(
        Modifier.fillMaxSize().systemBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HeadphoneImage(Modifier.height(220.dp).graphicsLayer { translationY = lift.dp.toPx() })
        Row(Modifier.padding(top = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(16.dp)) {
                val w = 2.dp.toPx()
                drawArc(Palette.track, 0f, FULL_TURN, false, style = Stroke(w))
                drawArc(Palette.ink, angle, SPINNER_SWEEP, false, style = Stroke(w))
            }
            Spacer(Modifier.width(10.dp))
            BasicText("Connecting to $name…", style = Type.body.copy(color = Palette.body))
        }
        Column(
            Modifier.padding(top = 34.dp).width(300.dp).graphicsLayer { alpha = pulse },
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(3) { Box(Modifier.fillMaxWidth().height(56.dp).background(Palette.skeleton, RoundedCornerShape(16.dp))) }
        }
    }
}

@Composable
fun EmptyScreen(
    title: String,
    message: String,
    action: String?,
    onAction: () -> Unit,
    secondary: Pair<String, () -> Unit>? = null,
) {
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 34.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HeadphoneImage(Modifier.height(200.dp), alpha = GREYED_ALPHA)
        Spacer(Modifier.height(26.dp))
        BasicText(title, style = Type.title.copy(textAlign = TextAlign.Center))
        Spacer(Modifier.height(6.dp))
        BasicText(message, style = Type.body.copy(color = Palette.secondary, textAlign = TextAlign.Center))
        Spacer(Modifier.height(24.dp))
        if (action != null) {
            PillButton(action, onAction)
            Spacer(Modifier.height(14.dp))
        }
        secondary?.let { (label, onClick) ->
            BasicText(
                label,
                style = Type.body.copy(color = Palette.blue),
                modifier = Modifier.padding(8.dp).then(Modifier.clickableNoRipple(onClick)),
            )
        }
    }
}
