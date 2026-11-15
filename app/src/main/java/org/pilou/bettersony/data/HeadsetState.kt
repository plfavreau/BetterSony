package org.pilou.bettersony.data

enum class ListenMode(val label: String) {
    NOISE_CANCELLING("Noise cancelling"),
    WIND("Wind reduction"),
    AMBIENT("Ambient"),
    OFF("Off"),
}

/** Noise cancelling / ambient sound. [level] is the ambient level, kept while in other modes. */
data class SoundControl(
    val mode: ListenMode = ListenMode.NOISE_CANCELLING,
    val level: Int = DEFAULT_AMBIENT,
    val voice: Boolean = false,
) {
    companion object {
        const val MIN_AMBIENT = 1
        const val MAX_AMBIENT = 20
        const val DEFAULT_AMBIENT = 10
    }
}

enum class Sensitivity(val label: String) { AUTO("Auto"), HIGH("High"), LOW("Low") }

enum class TalkTimeout(val label: String) { SHORT("15 s"), STANDARD("30 s"), LONG("1 min"), NEVER("Never") }

data class TalkConfig(
    val sensitivity: Sensitivity = Sensitivity.AUTO,
    val voiceFocus: Boolean = false,
    val timeout: TalkTimeout = TalkTimeout.STANDARD,
)

data class EqPreset(val code: Int, val name: String)

/** Clear Bass then the five bands, each -10..+10. */
data class Equalizer(val preset: Int = EQ_OFF, val bands: List<Int> = List(EQ_BANDS.size) { 0 }) {
    fun with(band: Int, value: Int): Equalizer = copy(bands = bands.toMutableList().also { it[band] = value })
    val isFlat: Boolean get() = bands.all { it == 0 }

    companion object {
        const val MIN = -10
        const val MAX = 10
    }
}

const val EQ_OFF = 0x00
const val EQ_MANUAL = 0xA0

/** Bands can only be edited on these; the others are fixed curves. */
val EDITABLE_PRESETS = listOf(EQ_MANUAL, 0xA1, 0xA2)
val EQ_BANDS = listOf("Clear Bass", "400", "1k", "2.5k", "6.3k", "16k")
val EQ_PRESETS = listOf(
    EqPreset(EQ_OFF, "Off"), EqPreset(0x10, "Bright"), EqPreset(0x11, "Excited"), EqPreset(0x12, "Mellow"),
    EqPreset(0x13, "Relaxed"), EqPreset(0x14, "Vocal"), EqPreset(0x15, "Treble Boost"), EqPreset(0x16, "Bass Boost"),
    EqPreset(0x17, "Speech"), EqPreset(EQ_MANUAL, "Manual"), EqPreset(0xA1, "Custom 1"), EqPreset(0xA2, "Custom 2"),
)

fun presetName(code: Int): String = EQ_PRESETS.firstOrNull { it.code == code }?.name ?: "Custom"

data class Headset(
    val name: String = "WH-1000XM4",
    val address: String = "",
    val firmware: String = "",
    val battery: Int? = null,
    val charging: Boolean = false,
    val codec: String? = null,
    val sound: SoundControl? = null,
    val speakToChat: Boolean? = null,
    val talk: TalkConfig? = null,
    val eq: Equalizer? = null,
    val dsee: Boolean? = null,
    val preferSound: Boolean? = null,
    val pauseWhenOff: Boolean? = null,
    val autoOff: Boolean? = null,
    val touch: Boolean? = null,
    val voiceGuide: Boolean? = null,
    val multipoint: Boolean? = null,
    val customButton: String? = null,
)

sealed interface ConnectionState {
    data object NeedsPermission : ConnectionState
    data object BluetoothOff : ConnectionState
    data class Disconnected(val canConnect: Boolean, val error: String? = null) : ConnectionState
    data class Connecting(val name: String) : ConnectionState
    /** [syncing] = showing the last known state while the link is (re)established. */
    data class Connected(val headset: Headset, val syncing: Boolean = false) : ConnectionState
}

val DEMO_HEADSET = Headset(
    name = "WH-1000XM4",
    address = "AC:80:0A:12:34:56",
    firmware = "3.0.1",
    battery = 80,
    codec = "AAC",
    sound = SoundControl(ListenMode.NOISE_CANCELLING),
    speakToChat = false,
    talk = TalkConfig(),
    eq = Equalizer(0xA2, listOf(-3, 0, 0, 0, 1, 2)),
    dsee = true,
    preferSound = true,
    pauseWhenOff = true,
    autoOff = true,
    touch = true,
    voiceGuide = true,
    multipoint = true,
    customButton = "Ambient sound",
)
