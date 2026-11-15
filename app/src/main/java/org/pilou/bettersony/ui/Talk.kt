package org.pilou.bettersony.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.pilou.bettersony.data.Sensitivity
import org.pilou.bettersony.data.TalkConfig
import org.pilou.bettersony.data.TalkTimeout

/** Speak-to-chat, the setting that pauses your music when you talk, plus the guard that keeps it the way you set it. */
@Composable
fun TalkCard(
    on: Boolean,
    config: TalkConfig?,
    keep: Boolean,
    onToggle: (Boolean) -> Unit,
    onKeep: (Boolean) -> Unit,
    onConfig: (TalkConfig) -> Unit,
) {
    Card(SectionPadding, verticalPadding = 4.dp) {
        Column {
            Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).background(if (on) Palette.blueTint else Palette.greenTint, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.talk, if (on) Palette.blue else Palette.green, size = 20.dp) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    BasicText("Speak-to-chat", style = Type.bodyStrong)
                    BasicText(
                        if (on) "Music pauses when you talk" else "Music keeps playing when you talk",
                        style = Type.callout.copy(fontSize = 13.sp),
                    )
                }
                Toggle(on, onToggle)
            }
            SettingsRow("Keep it this way", subtitle = "Set back every time your headphones connect, even with the app closed") {
                Toggle(keep, onKeep)
            }
            AnimatedVisibility(
                on && config != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                if (config != null) TalkOptions(config, onConfig)
            }
        }
    }
}

@Composable
private fun TalkOptions(config: TalkConfig, onConfig: (TalkConfig) -> Unit) {
    Column(Modifier.padding(bottom = 14.dp)) {
        SettingsRow("Voice detection") {}
        Segmented(
            options = Sensitivity.entries.map { it to it.label },
            selected = config.sensitivity,
            onSelect = { onConfig(config.copy(sensitivity = it)) },
        )
        SettingsRow("Resume music after", divider = false) {}
        Segmented(
            options = TalkTimeout.entries.map { it to it.label },
            selected = config.timeout,
            onSelect = { onConfig(config.copy(timeout = it)) },
        )
        Spacer(Modifier.size(6.dp))
        SettingsRow("Focus on voice", subtitle = "Lets voices through while it's paused") {
            Toggle(config.voiceFocus, { onConfig(config.copy(voiceFocus = it)) })
        }
    }
}
