package org.pilou.bettersony.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.pilou.bettersony.mdr.MdrClient
import org.pilou.bettersony.mdr.MdrFrame
import org.pilou.bettersony.mdr.MsgType
import java.io.IOException

private val CORE = listOf(Query.SOUND, Query.TALK, Query.BATTERY, Query.EQ)
private val DETAILS = listOf(
    Query.TALK_CONFIG, Query.CODEC, Query.DSEE, Query.CONNECTION_MODE, Query.PAUSE, Query.AUTO_OFF,
    Query.TOUCH, Query.MULTIPOINT, Query.BUTTON, Query.MODEL, Query.FIRMWARE,
)
private const val QUESTION_WAIT_MS = 3000L
private val EDITABLE = listOf(Query.SOUND, Query.TALK, Query.TALK_CONFIG, Query.EQ, Query.DSEE, Query.PAUSE, Query.AUTO_OFF)

/**
 * One open MDR connection. Every call runs on [io], a single-lane dispatcher, because
 * the underlying client is blocking and handles one command at a time.
 */
@SuppressLint("MissingPermission")
class HeadsetSession(
    val device: BluetoothDevice,
    private val io: CoroutineDispatcher,
    log: (String) -> Unit,
) {
    private val client = MdrClient(device, log)

    var onPush: ((MdrFrame) -> Unit)?
        get() = client.onUnsolicited
        set(value) { client.onUnsolicited = value }

    var onClosed: (() -> Unit)?
        get() = client.onClosed
        set(value) { client.onClosed = value }

    /** Opens the channel and runs the handshake the headset expects before anything else. */
    suspend fun open() = withContext(io) {
        client.connect()
        client.request(Query.INIT) ?: throw IOException("no reply to init")
    }

    fun close() = client.close()

    val isOpen: Boolean get() = client.isConnected

    private suspend fun get(payload: ByteArray, type: Int = MsgType.TABLE_1): MdrFrame? =
        withContext(io) { client.request(payload, type = type) }

    private suspend fun send(payload: ByteArray, type: Int = MsgType.TABLE_1) =
        withContext(io) { client.send(payload, type) }

    /** Reads what the home screen shows first; [readDetails] fills in the rest. */
    suspend fun readCore(): Headset {
        var h = Headset(name = device.name ?: "WH-1000XM4", address = device.address)
        for (q in CORE) get(q)?.let { h = h.apply(it) }
        return h
    }

    suspend fun readDetails(): List<MdrFrame> =
        DETAILS.mapNotNull { get(it) } + listOfNotNull(get(Query.VOICE_GUIDE, MsgType.TABLE_2))

    /** Re-reads what the user can change, to settle values edited while a sync was running. */
    suspend fun readEditable(): List<MdrFrame> = EDITABLE.mapNotNull { get(it) }

    suspend fun refreshBattery(): MdrFrame? = get(Query.BATTERY)

    suspend fun readTalk(): Boolean? = get(Query.TALK)?.takeIf { it.isTalkReport() }?.let { Headset().apply(it).speakToChat }

    /** Changes take effect on the ACK; the headset then echoes a notification the push pump applies. */
    suspend fun write(payload: ByteArray) = send(payload)

    suspend fun writeVoiceGuide(on: Boolean) = send(Command.voiceGuide(on), MsgType.TABLE_2)

    suspend fun powerOff() = send(Command.POWER_OFF)

    /**
     * For settings that restart the headset: it answers [payload] with a yes/no question and only
     * applies it once we say yes. Returns false if it never asked (nothing changed).
     */
    suspend fun writeWithRestart(payload: ByteArray): Boolean = withContext(io) {
        client.send(Command.ALERTS_ON)
        client.send(payload)
        val deadline = System.currentTimeMillis() + QUESTION_WAIT_MS
        var question: Int? = null
        while (question == null) {
            val left = deadline - System.currentTimeMillis()
            val frame = (if (left > 0) client.nextFrame(left) else null) ?: break
            question = frame.restartQuestion()
            if (question == null) client.onUnsolicited?.invoke(frame)
        }
        question?.let { client.send(Command.confirm(it)) }
        question != null
    }

    suspend fun pumpPushes() = withContext(io) { client.drainUnsolicited() }
}
