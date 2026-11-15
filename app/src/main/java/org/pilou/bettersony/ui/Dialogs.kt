package org.pilou.bettersony.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val SCRIM = Color(0x66000000)

/** Asks before turning the headphones off: there is no way to turn them back on from the phone. */
@Composable
fun PowerDialog(visible: Boolean, name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    BackHandler(visible, onDismiss)
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize().background(SCRIM).clickableNoRipple(onDismiss), contentAlignment = Alignment.Center) {
            AnimatedVisibility(visible, enter = scaleIn(initialScale = 0.92f), exit = scaleOut(targetScale = 0.92f)) {
                Column(
                    Modifier
                        .padding(horizontal = 36.dp)
                        .fillMaxWidth()
                        .background(Palette.surface, RoundedCornerShape(26.dp))
                        .clickableNoRipple {}
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(52.dp).background(Palette.track, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.power, Palette.ink, size = 24.dp)
                    }
                    Spacer(Modifier.height(14.dp))
                    BasicText("Turn off $name?", style = Type.title.copy(fontSize = Type.body.fontSize * 1.2f, textAlign = TextAlign.Center))
                    Spacer(Modifier.height(6.dp))
                    BasicText(
                        "Press the power button on the headphones to turn them back on.",
                        style = Type.callout.copy(textAlign = TextAlign.Center),
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DialogButton("Cancel", Palette.track, Palette.ink, Modifier.weight(1f), onDismiss)
                        DialogButton("Turn off", Palette.ink, Color.White, Modifier.weight(1f), onConfirm)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogButton(text: String, bg: Color, fg: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) { BasicText(text, style = Type.button.copy(color = fg)) }
}
