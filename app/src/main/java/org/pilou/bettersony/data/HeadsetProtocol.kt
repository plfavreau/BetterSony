package org.pilou.bettersony.data

import org.pilou.bettersony.mdr.MdrFrame
import org.pilou.bettersony.mdr.MsgType
import org.pilou.bettersony.mdr.bytesOf
import org.pilou.bettersony.mdr.u8

/** GET payloads used by the app. Every one is listed in docs/PROTOCOL.md. */
object Query {
    val INIT = bytesOf(0x00, 0x00)
    val MODEL = bytesOf(0x04, 0x01)
    val FIRMWARE = bytesOf(0x04, 0x02)
    val BATTERY = bytesOf(0x10, 0x00)
    val CODEC = bytesOf(0x18, 0x00)
    val SOUND = bytesOf(0x66, 0x02)
    val TALK = bytesOf(0xF6, 0x05)
    val TALK_CONFIG = bytesOf(0xFA, 0x05)
    val EQ = bytesOf(0x56, 0x01)
    val DSEE = bytesOf(0xE6, 0x02)
    val CONNECTION_MODE = bytesOf(0xE6, 0x01)
    val PAUSE = bytesOf(0xF6, 0x03)
    val AUTO_OFF = bytesOf(0xF6, 0x04)
    val TOUCH = bytesOf(0xD6, 0xD1)
    val MULTIPOINT = bytesOf(0xD6, 0xD2)
    val BUTTON = bytesOf(0xF6, 0x06)
    val VOICE_GUIDE = bytesOf(0x46, 0x01, 0x01)
}

private const val ON = 1
private const val OFF = 0
private const val EFFECT_ON = 0x11
private const val DUAL_SINGLE_OFF = 0x02
private const val LEVEL_ADJUST = 0x01
private const val NC_DUAL = 2
private const val NC_SINGLE = 1
private const val EQ_OFFSET = 10
private const val EQ_UNSPECIFIED = 0xFF
private const val AUTO_OFF_WHEN_REMOVED = 0x10
private const val AUTO_OFF_NEVER = 0x11
private const val SOUND_LEN = 8
private const val EQ_HEADER = 4

private fun flag(on: Boolean) = if (on) ON else OFF

object Command {
    fun sound(s: SoundControl): ByteArray {
        val nc = when (s.mode) {
            ListenMode.NOISE_CANCELLING -> NC_DUAL
            ListenMode.WIND -> NC_SINGLE
            else -> OFF
        }
        val ambient = s.mode == ListenMode.AMBIENT || s.mode == ListenMode.OFF
        return bytesOf(
            0x68, 0x02, if (s.mode == ListenMode.OFF) OFF else EFFECT_ON, DUAL_SINGLE_OFF, nc,
            LEVEL_ADJUST, flag(s.voice && ambient), if (ambient) s.level else 0,
        )
    }

    fun talk(on: Boolean) = bytesOf(0xF8, 0x05, 0x01, flag(on))
    fun talkConfig(c: TalkConfig) =
        bytesOf(0xFC, 0x05, 0x00, c.sensitivity.ordinal, flag(c.voiceFocus), c.timeout.ordinal)

    fun eqPreset(code: Int) = bytesOf(0x58, 0x01, code, 0x00)
    fun eqBands(bands: List<Int>) =
        bytesOf(0x58, 0x01, EQ_UNSPECIFIED, bands.size, *bands.map { it + EQ_OFFSET }.toIntArray())

    fun dsee(on: Boolean) = bytesOf(0xE8, 0x02, 0x00, flag(on))
    fun preferSound(on: Boolean) = bytesOf(0xE8, 0x01, 0x00, flag(!on))
    fun pause(on: Boolean) = bytesOf(0xF8, 0x03, 0x00, flag(on))
    fun autoOff(on: Boolean) = bytesOf(0xF8, 0x04, 0x01, if (on) AUTO_OFF_WHEN_REMOVED else AUTO_OFF_NEVER, 0x00)
    fun touch(on: Boolean) = bytesOf(0xD8, 0xD1, 0x01, flag(on))
    fun voiceGuide(on: Boolean) = bytesOf(0x48, 0x01, 0x01, flag(on))
    val POWER_OFF = bytesOf(0x22, 0x00, 0x01)
}

private val CODECS = mapOf(0x01 to "SBC", 0x02 to "AAC", 0x10 to "LDAC", 0x20 to "aptX", 0x21 to "aptX HD")
private val BUTTONS = mapOf(0x00 to "Ambient sound", 0x31 to "Google Assistant", 0x32 to "Alexa")

private fun ByteArray.at(i: Int): Int? = if (i < size) this[i].u8 else null
private fun ByteArray.bool(i: Int): Boolean? = at(i)?.let { it == ON }

private fun ByteArray.ascii(): String? {
    val len = at(2) ?: return null
    if (size < 3 + len) return null
    return String(this, 3, len, Charsets.US_ASCII)
}

private fun parseSound(p: ByteArray, old: SoundControl?): SoundControl? {
    if (p.size < SOUND_LEN) return null
    val mode = when {
        p[2].u8 == OFF -> ListenMode.OFF
        p[4].u8 == NC_DUAL -> ListenMode.NOISE_CANCELLING
        p[4].u8 == NC_SINGLE -> ListenMode.WIND
        else -> ListenMode.AMBIENT
    }
    val level = p[7].u8.takeIf { it in SoundControl.MIN_AMBIENT..SoundControl.MAX_AMBIENT }
    return SoundControl(mode, level ?: old?.level ?: SoundControl.DEFAULT_AMBIENT, if (level != null) p[6].u8 == ON else old?.voice ?: false)
}

private fun parseEq(p: ByteArray): Equalizer? {
    val count = p.at(3) ?: return null
    if (p.size < EQ_HEADER + count || count != EQ_BANDS.size) return null
    return Equalizer(p[2].u8, (0 until count).map { p[EQ_HEADER + it].u8 - EQ_OFFSET })
}

private fun parseTalkConfig(p: ByteArray): TalkConfig? {
    if (p.size < 6) return null
    return TalkConfig(
        Sensitivity.entries.getOrElse(p[3].u8) { Sensitivity.AUTO },
        p[4].u8 == ON,
        TalkTimeout.entries.getOrElse(p[5].u8) { TalkTimeout.STANDARD },
    )
}

/** Applies a reply or notification to the state; unknown frames leave it unchanged. */
fun Headset.apply(f: MdrFrame): Headset {
    val p = f.payload
    if (f.type == MsgType.TABLE_2) {
        return if (f.command in listOf(0x47, 0x49) && p.at(1) == 0x01 && p.at(2) == 0x01) copy(voiceGuide = p.bool(3) ?: voiceGuide) else this
    }
    return when (f.command to f.inquired) {
        0x05 to 0x01 -> copy(name = p.ascii() ?: name)
        0x05 to 0x02 -> copy(firmware = p.ascii() ?: firmware)
        0x11 to 0x00, 0x13 to 0x00 -> copy(battery = p.at(2)?.takeIf { it in 0..100 } ?: battery, charging = p.bool(3) ?: charging)
        0x19 to 0x00, 0x1B to 0x00 -> copy(codec = p.at(2)?.let { CODECS[it] } ?: codec)
        0x67 to 0x02, 0x69 to 0x02 -> copy(sound = parseSound(p, sound) ?: sound)
        0xF7 to 0x05, 0xF9 to 0x05 -> if (p.at(2) in listOf(0x00, 0x01)) copy(speakToChat = p.bool(3) ?: speakToChat) else this
        0xFB to 0x05, 0xFD to 0x05 -> copy(talk = parseTalkConfig(p) ?: talk)
        0x57 to 0x01, 0x59 to 0x01 -> copy(eq = parseEq(p) ?: eq)
        0xE7 to 0x02, 0xE9 to 0x02 -> copy(dsee = p.bool(3) ?: dsee)
        0xE7 to 0x01, 0xE9 to 0x01 -> copy(preferSound = p.at(3)?.let { it == OFF } ?: preferSound)
        0xF7 to 0x03, 0xF9 to 0x03 -> copy(pauseWhenOff = p.bool(3) ?: pauseWhenOff)
        0xF7 to 0x04, 0xF9 to 0x04 -> copy(autoOff = p.at(3)?.let { it == AUTO_OFF_WHEN_REMOVED } ?: autoOff)
        0xD7 to 0xD1, 0xD9 to 0xD1 -> copy(touch = p.bool(3) ?: touch)
        0xD7 to 0xD2, 0xD9 to 0xD2 -> copy(multipoint = p.bool(3) ?: multipoint)
        0xF7 to 0x06, 0xF9 to 0x06 -> copy(customButton = p.at(3)?.let { BUTTONS[it] ?: "Custom" } ?: customButton)
        else -> this
    }
}

/** True for a speak-to-chat on/off report (not the preview or status ones). */
fun MdrFrame.isTalkReport(): Boolean =
    type == MsgType.TABLE_1 && command in listOf(0xF7, 0xF9) && inquired == 0x05 && payload.at(2) in listOf(0x00, 0x01)
