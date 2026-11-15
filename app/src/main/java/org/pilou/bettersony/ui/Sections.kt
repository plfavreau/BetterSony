package org.pilou.bettersony.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.pilou.bettersony.data.EDITABLE_PRESETS
import org.pilou.bettersony.data.EQ_BANDS
import org.pilou.bettersony.data.EQ_PRESETS
import org.pilou.bettersony.data.Equalizer
import org.pilou.bettersony.data.ListenMode
import org.pilou.bettersony.data.SoundControl

val SectionPadding = Modifier.padding(start = 18.dp, end = 18.dp, top = 22.dp)
private val MODE_LABELS = mapOf(
    ListenMode.NOISE_CANCELLING to "Noise\ncancelling",
    ListenMode.WIND to "Wind\nreduction",
    ListenMode.AMBIENT to "Ambient\nsound",
    ListenMode.OFF to "Off\n",
)

@Composable
fun ModeRow(current: ListenMode, onSelect: (ListenMode) -> Unit) {
    Column(SectionPadding) {
        SectionLabel("Ambient sound control")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ListenMode.entries.forEach { mode ->
                ModeCard(mode, mode == current, Modifier.weight(1f)) { onSelect(mode) }
            }
        }
    }
}

@Composable
private fun ModeCard(mode: ListenMode, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Palette.ink else Color.White, label = "modeBg")
    val fg by animateColorAsState(if (selected) Color.White else Palette.ink, label = "modeFg")
    Column(
        modifier
            .shadow(1.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x0A000000), spotColor = Color(0x0A000000))
            .background(bg, RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(start = 12.dp, end = 8.dp, top = 14.dp, bottom = 12.dp),
    ) {
        Icon(Icons.forMode(mode), fg)
        Spacer(Modifier.height(16.dp))
        BasicText(MODE_LABELS.getValue(mode), style = Type.bodyStrong.copy(fontSize = Type.caption.fontSize, color = fg), maxLines = 2)
    }
}

@Composable
fun AmbientCard(sound: SoundControl, onCommit: (SoundControl) -> Unit) {
    AnimatedVisibility(
        sound.mode == ListenMode.AMBIENT,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        var drag by remember(sound.level) { mutableStateOf(sound.level) }
        Card(Modifier.padding(start = 18.dp, end = 18.dp, top = 10.dp)) {
            Column {
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    BasicText("Ambient level", style = Type.body)
                    BasicText("$drag", style = Type.label)
                }
                StepSlider(
                    value = drag,
                    range = SoundControl.MIN_AMBIENT..SoundControl.MAX_AMBIENT,
                    onChange = { drag = it },
                    onCommit = { onCommit(sound.copy(level = it)) },
                )
                SettingsRow("Focus on voice", subtitle = "Lets voices through, keeps the rest down") {
                    Toggle(sound.voice, { onCommit(sound.copy(voice = it)) })
                }
            }
        }
    }
}

@Composable
fun EqualizerCard(eq: Equalizer, onPreset: (Int) -> Unit, onBand: (Int, Int) -> Unit, onReset: () -> Unit) {
    Column(Modifier.padding(top = 22.dp)) {
        SectionLabel("Equalizer", Modifier.padding(horizontal = 18.dp))
        val selected = EQ_PRESETS.indexOfFirst { it.code == eq.preset }.coerceAtLeast(0)
        LazyRow(
            state = rememberLazyListState(initialFirstVisibleItemIndex = (selected - 1).coerceAtLeast(0)),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(EQ_PRESETS, key = { it.code }) { p -> Chip(p.name, p.code == eq.preset) { onPreset(p.code) } }
        }
        val editable = eq.preset in EDITABLE_PRESETS
        Card(Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp)) {
            Column {
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    BasicText("Bands", style = Type.body)
                    if (editable && !eq.isFlat) {
                        BasicText(
                            "Reset",
                            style = Type.label.copy(color = Palette.blue),
                            modifier = Modifier.clickable(role = Role.Button, onClick = onReset),
                        )
                    }
                }
                EQ_BANDS.forEachIndexed { i, label -> EqRow(label, eq.bands.getOrElse(i) { 0 }, editable) { onBand(i, it) } }
                if (!editable) {
                    BasicText(
                        "Pick Manual or a Custom preset to adjust the bands.",
                        style = Type.caption.copy(color = Palette.secondary),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Palette.ink else Color.White, label = "chipBg")
    val fg by animateColorAsState(if (selected) Color.White else Palette.ink, label = "chipFg")
    BasicText(
        text,
        style = Type.bodyStrong.copy(fontSize = Type.callout.fontSize, color = fg),
        modifier = Modifier
            .shadow(1.dp, CircleShape, ambientColor = Color(0x0A000000), spotColor = Color(0x0A000000))
            .background(bg, CircleShape)
            .clip(CircleShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}

@Composable
private fun EqRow(label: String, value: Int, enabled: Boolean, onCommit: (Int) -> Unit) {
    var drag by remember(value) { mutableStateOf(value) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText(label, style = Type.callout.copy(color = Palette.body), modifier = Modifier.width(78.dp))
        StepSlider(
            drag, Equalizer.MIN..Equalizer.MAX, onChange = { drag = it }, onCommit = onCommit,
            modifier = Modifier.weight(1f), enabled = enabled, origin = 0,
        )
        BasicText(
            if (drag > 0) "+$drag" else "$drag",
            style = Type.bodyStrong.copy(fontSize = Type.callout.fontSize, textAlign = TextAlign.End),
            modifier = Modifier.width(34.dp),
        )
    }
}
