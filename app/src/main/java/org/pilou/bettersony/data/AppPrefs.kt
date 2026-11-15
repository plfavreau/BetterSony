package org.pilou.bettersony.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import org.pilou.bettersony.widget.WidgetStore

data class AlertSettings(val lowBattery: Boolean = true, val threshold: Int = DEFAULT_THRESHOLD) {
    companion object {
        const val DEFAULT_THRESHOLD = 20
        val THRESHOLDS = listOf(10, 20, 30)
    }
}

/** Small persisted state: alert settings, alert bookkeeping and the last headset snapshot. */
class AppPrefs private constructor(private val context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("sony", Context.MODE_PRIVATE)

    private val _alerts = MutableStateFlow(
        AlertSettings(sp.getBoolean(K_LOW, true), sp.getInt(K_THRESHOLD, AlertSettings.DEFAULT_THRESHOLD)),
    )
    val alerts: StateFlow<AlertSettings> = _alerts.asStateFlow()

    fun setAlerts(settings: AlertSettings) {
        sp.edit().putBoolean(K_LOW, settings.lowBattery).putInt(K_THRESHOLD, settings.threshold).apply()
        _alerts.value = settings
    }

    /** Lowest alert step (e.g. 20, 10, 5) already notified for this discharge; 0 = none yet. */
    var lowAlertStep: Int
        get() = sp.getInt(K_STEP, 0)
        set(value) = sp.edit().putInt(K_STEP, value).apply()

    private val _keepTalk = MutableStateFlow(sp.getBoolean(K_KEEP_TALK, true))

    /** Restore speak-to-chat to [talkChoice] on every connection, even with the app closed. */
    val keepTalk: StateFlow<Boolean> = _keepTalk.asStateFlow()

    fun setKeepTalk(on: Boolean) {
        sp.edit().putBoolean(K_KEEP_TALK, on).apply()
        _keepTalk.value = on
    }

    /** Last speak-to-chat choice made in the app. Off until you turn it on here. */
    var talkChoice: Boolean
        get() = sp.getBoolean(K_TALK, false)
        set(value) = sp.edit().putBoolean(K_TALK, value).apply()

    /** What speak-to-chat should be restored to, or null when you let the headphones decide. */
    val talkWanted: Boolean? get() = talkChoice.takeIf { keepTalk.value }

    var askedNotifications: Boolean
        get() = sp.getBoolean(K_ASKED, false)
        set(value) = sp.edit().putBoolean(K_ASKED, value).apply()

    fun saveHeadset(h: Headset) {
        sp.edit().putString(K_HEADSET, h.toJson().toString()).apply()
        WidgetStore.refresh(context)
    }

    /** Something widgets show changed outside the snapshot (e.g. the link dropped). */
    fun touch() = WidgetStore.refresh(context)

    fun loadHeadset(): Headset? = sp.getString(K_HEADSET, null)?.let { runCatching { headsetFromJson(JSONObject(it)) }.getOrNull() }

    companion object {
        private const val K_LOW = "alert_low"
        private const val K_THRESHOLD = "alert_threshold"
        private const val K_STEP = "alert_low_step"
        private const val K_ASKED = "asked_notifications"
        private const val K_HEADSET = "headset"
        private const val K_KEEP_TALK = "keep_talk"
        private const val K_TALK = "talk_choice"

        @Volatile private var instance: AppPrefs? = null

        fun get(context: Context): AppPrefs =
            instance ?: synchronized(this) { instance ?: AppPrefs(context.applicationContext).also { instance = it } }
    }
}

private fun Headset.toJson(): JSONObject = JSONObject().apply {
    put("name", name); put("address", address); put("firmware", firmware); put("charging", charging)
    battery?.let { put("battery", it) }
    codec?.let { put("codec", it) }
    sound?.let { put("sound", JSONObject().put("m", it.mode.name).put("l", it.level).put("v", it.voice)) }
    talk?.let { put("talk", JSONObject().put("s", it.sensitivity.name).put("v", it.voiceFocus).put("t", it.timeout.name)) }
    eq?.let { put("eq", JSONObject().put("p", it.preset).put("b", JSONArray(it.bands))) }
    customButton?.let { put("button", it) }
    listOf("stc" to speakToChat, "dsee" to dsee, "pq" to preferSound, "pause" to pauseWhenOff, "auto" to autoOff,
        "touch" to touch, "voice" to voiceGuide, "multi" to multipoint).forEach { (k, v) -> v?.let { put(k, it) } }
}

private fun JSONObject.intOrNull(k: String): Int? = if (has(k)) getInt(k) else null
private fun JSONObject.boolOrNull(k: String): Boolean? = if (has(k)) getBoolean(k) else null
private fun JSONObject.strOrNull(k: String): String? = if (has(k)) getString(k) else null

private fun headsetFromJson(o: JSONObject): Headset {
    val sound = o.optJSONObject("sound")?.let { SoundControl(ListenMode.valueOf(it.getString("m")), it.getInt("l"), it.getBoolean("v")) }
    val talk = o.optJSONObject("talk")?.let {
        TalkConfig(Sensitivity.valueOf(it.getString("s")), it.getBoolean("v"), TalkTimeout.valueOf(it.getString("t")))
    }
    val eq = o.optJSONObject("eq")?.let { e ->
        val b = e.getJSONArray("b")
        Equalizer(e.getInt("p"), (0 until b.length()).map(b::getInt))
    }
    return Headset(
        name = o.optString("name"), address = o.optString("address"), firmware = o.optString("firmware"),
        battery = o.intOrNull("battery"), charging = o.optBoolean("charging"), codec = o.strOrNull("codec"),
        sound = sound, speakToChat = o.boolOrNull("stc"), talk = talk, eq = eq, dsee = o.boolOrNull("dsee"),
        preferSound = o.boolOrNull("pq"), pauseWhenOff = o.boolOrNull("pause"), autoOff = o.boolOrNull("auto"),
        touch = o.boolOrNull("touch"), voiceGuide = o.boolOrNull("voice"), multipoint = o.boolOrNull("multi"),
        customButton = o.strOrNull("button"),
    )
}
