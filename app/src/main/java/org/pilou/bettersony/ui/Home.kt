package org.pilou.bettersony.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.pilou.bettersony.data.Headset
import org.pilou.bettersony.data.HeadsetRepository
import org.pilou.bettersony.data.resetEq
import org.pilou.bettersony.data.setAutoOff
import org.pilou.bettersony.data.setDsee
import org.pilou.bettersony.data.setEqBand
import org.pilou.bettersony.data.setEqPreset
import org.pilou.bettersony.data.setPauseWhenOff
import org.pilou.bettersony.data.setPreferSound
import org.pilou.bettersony.data.setSound
import org.pilou.bettersony.data.setTalk
import org.pilou.bettersony.data.setTalkConfig
import org.pilou.bettersony.data.setTouch
import org.pilou.bettersony.data.setVoiceGuide

@Composable
fun HomeContent(
    headset: Headset,
    syncing: Boolean,
    phone: String,
    repo: HeadsetRepository,
    keepTalk: Boolean,
    onKeepTalk: (Boolean) -> Unit,
    onPower: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        Modifier
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
    ) {
        Header(headset.name, phone, syncing, onPower, onSettings)
        Hero(headset.battery, headset.charging, codecLabel(headset))
        headset.battery?.takeIf { it <= LOW_BATTERY && !headset.charging }?.let { LowBatteryNotice(it) }
        headset.speakToChat?.let { on ->
            TalkCard(on, headset.talk, keepTalk, { repo.setTalk(it) }, onKeepTalk, repo::setTalkConfig)
        }
        headset.sound?.let { sound ->
            ModeRow(sound.mode) { repo.setSound(sound.copy(mode = it)) }
            AmbientCard(sound, repo::setSound)
        }
        headset.eq?.let { EqualizerCard(it, repo::setEqPreset, repo::setEqBand, repo::resetEq) }
        SoundCard(headset, repo)
        HeadphonesCard(headset, repo)
        Spacer(Modifier.height(40.dp))
    }
}

private fun codecLabel(h: Headset): String? =
    listOfNotNull(h.codec, "DSEE Extreme".takeIf { h.dsee == true }).joinToString("  ·  ").ifEmpty { null }

@Composable
private fun SoundCard(headset: Headset, repo: HeadsetRepository) {
    if (headset.dsee == null && headset.preferSound == null) return
    Column(SectionPadding) {
        SectionLabel("Sound")
        Card(verticalPadding = 4.dp) {
            Column {
                headset.dsee?.let { on ->
                    SettingsRow("DSEE Extreme", divider = false, subtitle = "Restores detail lost in compressed music") {
                        Toggle(on, repo::setDsee)
                    }
                }
                headset.preferSound?.let { on ->
                    SettingsRow("Prioritize sound quality", divider = headset.dsee != null, subtitle = "Off favors a stable connection") {
                        Toggle(on, repo::setPreferSound)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeadphonesCard(headset: Headset, repo: HeadsetRepository) {
    Column(SectionPadding) {
        SectionLabel("Headphones")
        Card(verticalPadding = 4.dp) {
            Column {
                var first = true
                fun divider(): Boolean = (!first).also { first = false }
                headset.pauseWhenOff?.let { on ->
                    SettingsRow("Pause when taken off", divider = divider()) { Toggle(on, repo::setPauseWhenOff) }
                }
                headset.autoOff?.let { on ->
                    SettingsRow("Turn off when taken off", divider = divider(), subtitle = "Saves battery when you are not wearing them") {
                        Toggle(on, repo::setAutoOff)
                    }
                }
                headset.touch?.let { on ->
                    SettingsRow("Touch controls", divider = divider()) { Toggle(on, repo::setTouch) }
                }
                headset.voiceGuide?.let { on ->
                    SettingsRow("Voice prompts", divider = divider()) { Toggle(on, repo::setVoiceGuide) }
                }
                headset.customButton?.let {
                    SettingsRow("Custom button", divider = divider()) { BasicText(it, style = Type.callout) }
                }
                headset.multipoint?.let {
                    SettingsRow("Two devices at once", divider = divider()) { BasicText(if (it) "On" else "Off", style = Type.callout) }
                }
            }
        }
    }
}
