package org.pilou.bettersony.data

import android.bluetooth.BluetoothDevice
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.pilou.bettersony.alerts.BatteryCheckJob
import org.pilou.bettersony.mdr.MdrFrame

private const val TAG = "BetterSony"
private const val PUMP_INTERVAL_MS = 400L
private const val BATTERY_POLL_MS = 60_000L
private const val A2DP_SETTLE_MS = 1200L
private const val RECONNECT_DELAY_MS = 2000L
private const val CONNECT_TIMEOUT_MS = 30_000L
private const val CONNECT_RETRIES = 3
private const val RETRY_DELAY_MS = 3000L
private val BATTERY_REPORTS = listOf(0x11, 0x13)

/** Owns the connection lifecycle and exposes one observable [ConnectionState]. */
@OptIn(ExperimentalCoroutinesApi::class)
class HeadsetRepository(
    private val devices: SonyDevices,
    private val prefs: AppPrefs,
    private val scope: CoroutineScope,
    private val onBattery: (Int) -> Unit,
) {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected(canConnect = false))
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private var session: HeadsetSession? = null
    private var sessionJob: Job? = null
    private var eventsJob: Job? = null
    private var editsDuringSync = 0

    private fun log(msg: String) = Log.i(TAG, msg)

    fun start() {
        if (eventsJob == null) {
            eventsJob = scope.launch { devices.events().collect(::onEvent) }
        }
        if (session == null) scope.launch { autoConnect() }
    }

    fun stop() {
        eventsJob?.cancel()
        eventsJob = null
        dropSession()
    }

    /** Debug only: renders the connected screen with sample data, no headset needed. */
    fun showDemo(battery: Int) {
        eventsJob?.cancel()
        _state.value = ConnectionState.Connected(DEMO_HEADSET.copy(battery = battery))
    }

    fun currentBattery(): Int? = (_state.value as? ConnectionState.Connected)?.headset?.battery

    fun snapshot(): Headset? = (_state.value as? ConnectionState.Connected)?.headset ?: prefs.loadHeadset()

    fun requirePermission() { _state.value = ConnectionState.NeedsPermission }

    private suspend fun autoConnect() {
        if (!devices.isEnabled) {
            _state.value = ConnectionState.BluetoothOff
            return
        }
        val device = devices.connected()
        if (device != null) connect(device) else idle()
    }

    private fun idle(error: String? = null) {
        _state.value = if (devices.isEnabled) {
            ConnectionState.Disconnected(canConnect = devices.bonded().isNotEmpty(), error = error)
        } else {
            ConnectionState.BluetoothOff
        }
    }

    /** Manual connect: opening RFCOMM pages the headset, which then brings its audio back too. */
    fun connectManually() {
        scope.launch {
            val device = devices.connected() ?: devices.bonded().firstOrNull()
            if (device == null) idle("No Sony headphones are paired with this phone.") else connect(device)
        }
    }

    private suspend fun onEvent(event: BtEvent) {
        when (event) {
            is BtEvent.AdapterChanged -> if (event.on) autoConnect() else { dropSession(); idle() }
            is BtEvent.HeadsetConnected -> if (session == null) {
                delay(A2DP_SETTLE_MS)
                if (session == null) connect(event.device)
            }
            is BtEvent.HeadsetDisconnected -> if (session?.device?.address == event.device.address) {
                dropSession()
                idle()
            }
        }
    }

    private suspend fun connect(device: BluetoothDevice, attempt: Int = 1) {
        if (session != null) return
        val cached = ((_state.value as? ConnectionState.Connected)?.headset ?: prefs.loadHeadset())
            ?.takeIf { it.address == device.address }
        _state.value = if (cached != null) ConnectionState.Connected(cached, syncing = true) else ConnectionState.Connecting(device.name ?: "WH-1000XM4")
        // Own lane per session: a stuck old session must never block a new one.
        val s = HeadsetSession(device, Dispatchers.IO.limitedParallelism(1), ::log)
        session = s
        BatteryCheckJob.appConnected = true
        // Blocking socket calls ignore coroutine cancellation, so the timeout closes the session instead.
        val watchdog = scope.launch { delay(CONNECT_TIMEOUT_MS); log("connect timed out"); s.close() }
        try {
            s.open()
            if (session !== s) return s.close()
            s.onPush = { frame -> onPush(frame) }
            editsDuringSync = 0
            var headset = s.readCore()
            cached?.let { headset = headset.withDetailsFrom(it) }
            // Taps made while syncing already reached the headset, but the reads above may predate them.
            if (editsDuringSync > 0) s.readEditable().forEach { headset = headset.apply(it) }
            watchdog.cancel()
            if (session !== s || !s.isOpen) throw java.io.IOException("link lost while reading")
            s.onClosed = { scope.launch { onSessionClosed(s) } }
            _state.value = ConnectionState.Connected(headset)
            prefs.saveHeadset(headset)
            headset.battery?.let(onBattery)
            sessionJob = scope.launch {
                guardTalk(s)
                readDetails(s)
                maintain(s)
            }
        } catch (e: Exception) {
            watchdog.cancel()
            log("connect attempt $attempt failed: ${e.message}")
            s.close()
            if (session !== s) return
            session = null
            BatteryCheckJob.appConnected = false
            val stillThere = devices.connected()?.address == device.address
            if (stillThere && attempt < CONNECT_RETRIES) {
                delay(RETRY_DELAY_MS)
                if (session == null) connect(device, attempt + 1)
            } else {
                idle("Couldn't reach the headphones. Make sure they're on and nearby.")
            }
        }
    }

    /** Speak-to-chat back to the choice made in this app; headsets and other apps turn it on behind your back. */
    private suspend fun guardTalk(s: HeadsetSession) {
        val wanted = prefs.talkWanted ?: return
        val now = (_state.value as? ConnectionState.Connected)?.headset?.speakToChat ?: return
        if (now == wanted || session !== s) return
        log("speak-to-chat was $now, restoring $wanted")
        setTalk(wanted, remember = false)
    }

    /** Reads speak-to-chat again and restores it if something changed it since connecting. */
    suspend fun recheckTalk() {
        val s = session ?: return
        val wanted = prefs.talkWanted ?: return
        val now = runCatching { s.readTalk() }.getOrNull() ?: return
        edit { it.copy(speakToChat = now) }
        if (now != wanted && session === s) {
            log("speak-to-chat turned $now after connecting, restoring $wanted")
            setTalk(wanted, remember = false)
        }
    }

    private suspend fun readDetails(s: HeadsetSession) {
        val frames = runCatching { s.readDetails() }.getOrNull() ?: return
        if (session !== s) return
        edit { h -> frames.fold(h) { acc, f -> acc.apply(f) } }
        (_state.value as? ConnectionState.Connected)?.let { prefs.saveHeadset(it.headset) }
    }

    private suspend fun maintain(s: HeadsetSession) {
        var sinceBattery = 0L
        while (scope.isActive && session === s) {
            delay(PUMP_INTERVAL_MS)
            s.pumpPushes()
            sinceBattery += PUMP_INTERVAL_MS
            if (sinceBattery >= BATTERY_POLL_MS) {
                sinceBattery = 0
                s.refreshBattery()?.let(::onPush)
            }
        }
    }

    private suspend fun onSessionClosed(s: HeadsetSession) {
        if (session !== s) return
        dropSession()
        delay(RECONNECT_DELAY_MS)
        if (session == null) autoConnect()
    }

    private fun dropSession() {
        sessionJob?.cancel()
        sessionJob = null
        session?.let { it.onClosed = null; it.close() }
        session = null
        BatteryCheckJob.appConnected = false
        prefs.touch()
    }

    private fun onPush(frame: MdrFrame) {
        _state.update { st -> if (st is ConnectionState.Connected) st.copy(headset = st.headset.apply(frame)) else st }
        if (frame.command in BATTERY_REPORTS) currentBattery()?.let(onBattery)
        (_state.value as? ConnectionState.Connected)?.takeUnless { it.syncing }?.let { prefs.saveHeadset(it.headset) }
    }

    private fun edit(transform: (Headset) -> Headset) {
        if ((_state.value as? ConnectionState.Connected)?.syncing == true) editsDuringSync++
        _state.update { st -> if (st is ConnectionState.Connected) st.copy(headset = transform(st.headset)) else st }
    }

    /** Optimistically applies [optimistic], then sends; the echo (or a re-read) settles the truth. */
    private fun write(optimistic: (Headset) -> Headset, send: suspend (HeadsetSession) -> Unit) {
        edit(optimistic)
        val s = session ?: return
        scope.launch {
            runCatching { send(s); s.pumpPushes() }.onFailure { log("write failed: ${it.message}") }
        }
    }

    /** Opens the link if needed (widget taps); commands sent right after queue behind the connect. */
    suspend fun ensureLinked(): Boolean {
        if (session != null) return true
        val device = devices.connected() ?: return false
        if (session == null) scope.launch(start = CoroutineStart.UNDISPATCHED) { connect(device) }
        return session != null
    }

    fun setSound(sound: SoundControl) = write({ it.copy(sound = sound) }) { it.write(Command.sound(sound)) }

    /** [remember] = a choice made in the app, kept and restored on every connection. */
    fun setTalk(on: Boolean, remember: Boolean = true) {
        if (remember) prefs.talkChoice = on
        write({ it.copy(speakToChat = on) }) { it.write(Command.talk(on)) }
    }

    fun setTalkConfig(c: TalkConfig) = write({ it.copy(talk = c) }) { it.write(Command.talkConfig(c)) }

    fun setEqPreset(code: Int) = write({ h -> h.copy(eq = (h.eq ?: Equalizer()).copy(preset = code)) }) {
        it.write(Command.eqPreset(code))
    }

    fun setEqBand(band: Int, value: Int) {
        val bands = ((_state.value as? ConnectionState.Connected)?.headset?.eq ?: Equalizer()).with(band, value).bands
        write({ h -> h.copy(eq = (h.eq ?: Equalizer()).copy(bands = bands)) }) { it.write(Command.eqBands(bands)) }
    }

    fun resetEq() {
        val flat = Equalizer().bands
        write({ h -> h.copy(eq = (h.eq ?: Equalizer()).copy(bands = flat)) }) { it.write(Command.eqBands(flat)) }
    }

    fun setDsee(on: Boolean) = write({ it.copy(dsee = on) }) { it.write(Command.dsee(on)) }

    fun setPreferSound(on: Boolean) = write({ it.copy(preferSound = on) }) { it.write(Command.preferSound(on)) }

    fun setPauseWhenOff(on: Boolean) = write({ it.copy(pauseWhenOff = on) }) { it.write(Command.pause(on)) }

    fun setAutoOff(on: Boolean) = write({ it.copy(autoOff = on) }) { it.write(Command.autoOff(on)) }

    fun setTouch(on: Boolean) = write({ it.copy(touch = on) }) { it.write(Command.touch(on)) }

    fun setVoiceGuide(on: Boolean) = write({ it.copy(voiceGuide = on) }) { it.writeVoiceGuide(on) }

    /** Turns the headphones off. The link drops right after, which resets the state to disconnected. */
    suspend fun powerOff(): Boolean {
        val s = session ?: return false
        return runCatching { s.powerOff() }.onFailure { log("power off failed: ${it.message}") }.isSuccess
    }
}

/** Keeps the slower-to-read fields from the cached snapshot until the fresh ones arrive. */
private fun Headset.withDetailsFrom(c: Headset): Headset = copy(
    name = c.name.ifEmpty { name }, firmware = c.firmware, codec = c.codec, talk = c.talk, dsee = c.dsee,
    preferSound = c.preferSound, pauseWhenOff = c.pauseWhenOff, autoOff = c.autoOff, touch = c.touch,
    voiceGuide = c.voiceGuide, multipoint = c.multipoint, customButton = c.customButton,
)
