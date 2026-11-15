package org.pilou.bettersony.mdr

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.util.Timer
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

/**
 * Blocking MDR client over one RFCOMM socket. One command is in flight at a time:
 * each is retried until the headset ACKs it, and every frame the headset sends is ACKed
 * right away. Replies are matched by command byte, since notifications can arrive anytime.
 */
@SuppressLint("MissingPermission")
class MdrClient(
    private val device: BluetoothDevice,
    private val log: (String) -> Unit,
) {
    @Volatile private var socket: BluetoothSocket? = null
    @Volatile private var pending: BluetoothSocket? = null
    @Volatile private var closed = false
    private val inbox = LinkedBlockingQueue<MdrFrame>()
    private val acks = LinkedBlockingQueue<MdrFrame>()
    private val writeLock = Any()
    private val commandLock = Any()
    private var seq = 0

    var onUnsolicited: ((MdrFrame) -> Unit)? = null
    var onClosed: (() -> Unit)? = null

    val isConnected: Boolean get() = socket?.isConnected == true

    fun connect(): String {
        val strategies: List<Pair<String, () -> BluetoothSocket>> = listOf(
            "secure" to { device.createRfcommSocketToServiceRecord(SERVICE_UUID) },
            "insecure" to { device.createInsecureRfcommSocketToServiceRecord(SERVICE_UUID) },
        )
        var lastError: Exception? = null
        for ((label, factory) in strategies) {
            for (attempt in 1..CONNECT_ATTEMPTS) {
                if (closed) throw IOException("closed while connecting")
                try {
                    val s = factory()
                    pending = s
                    // BluetoothSocket.connect has no timeout of its own and can hang on a busy link.
                    val watchdog = Timer(true).apply { schedule(timerTask { runCatching { s.close() } }, ATTEMPT_TIMEOUT_MS) }
                    try { s.connect() } finally { watchdog.cancel() }
                    pending = null
                    if (closed) {
                        runCatching { s.close() }
                        throw IOException("closed while connecting")
                    }
                    socket = s
                    startReader(s)
                    log("connected ($label, attempt $attempt)")
                    return label
                } catch (e: Exception) {
                    if (closed) throw IOException("closed while connecting")
                    lastError = e
                    log("connect $label #$attempt failed: ${e.javaClass.simpleName}: ${e.message}")
                    Thread.sleep(BACKOFF_MS * attempt)
                }
            }
        }
        throw IOException("could not open the control channel", lastError)
    }

    private fun startReader(s: BluetoothSocket) {
        Thread({
            val parser = MdrStreamParser()
            val buf = ByteArray(READ_BUFFER)
            try {
                val input = s.inputStream
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    for (frame in parser.feed(buf, n)) {
                        log("<- $frame")
                        if (frame.type == MsgType.ACK) {
                            acks.put(frame)
                        } else {
                            write(MdrFrame(MsgType.ACK, 1 - frame.seq))
                            inbox.put(frame)
                        }
                    }
                }
            } catch (e: IOException) {
                log("reader stopped: ${e.message}")
            }
            onClosed?.invoke()
        }, "mdr-reader").apply { isDaemon = true; start() }
    }

    private fun write(frame: MdrFrame) {
        val s = socket ?: throw IOException("not connected")
        synchronized(writeLock) {
            s.outputStream.write(frame.encode())
            s.outputStream.flush()
        }
    }

    /** Sends one command and waits for its ACK, resending with the same seq if it is lost. */
    fun send(payload: ByteArray, type: Int = MsgType.TABLE_1) {
        synchronized(commandLock) {
            acks.clear()
            val frame = MdrFrame(type, seq, payload)
            repeat(SEND_ATTEMPTS) {
                log("-> $frame")
                write(frame)
                val deadline = System.currentTimeMillis() + ACK_TIMEOUT_MS
                while (true) {
                    val left = deadline - System.currentTimeMillis()
                    if (left <= 0) break
                    val ack = acks.poll(left, TimeUnit.MILLISECONDS) ?: break
                    if (ack.seq != seq) {
                        seq = ack.seq
                        return
                    }
                }
            }
            throw IOException("no ACK for ${payload.hex()}")
        }
    }

    /**
     * Sends [payload] and returns the first reply whose command byte is [reply] and whose
     * second byte matches the request's, or null on timeout.
     */
    fun request(
        payload: ByteArray,
        reply: Int = payload[0].u8 + 1,
        type: Int = MsgType.TABLE_1,
        timeoutMs: Long = REPLY_TIMEOUT_MS,
    ): MdrFrame? {
        drainUnsolicited()
        send(payload, type)
        val inquired = if (payload.size > 1) payload[1].u8 else -1
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            val left = deadline - System.currentTimeMillis()
            if (left <= 0) return null
            val f = inbox.poll(left, TimeUnit.MILLISECONDS) ?: return null
            if (f.command == reply && (inquired < 0 || f.inquired == inquired)) return f
            onUnsolicited?.invoke(f)
        }
    }

    /** Waits for any headset-initiated frame, used to collect notifications after a SET. */
    fun nextFrame(timeoutMs: Long): MdrFrame? = inbox.poll(timeoutMs, TimeUnit.MILLISECONDS)

    fun drainUnsolicited() {
        while (true) {
            val f = inbox.poll() ?: return
            onUnsolicited?.invoke(f)
        }
    }

    fun close() {
        closed = true
        try { pending?.close() } catch (_: IOException) {}
        try { socket?.close() } catch (_: IOException) {}
        socket = null
    }

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("96CC203E-5068-46AD-B32D-E316F5E069BA")
        private const val CONNECT_ATTEMPTS = 2
        private const val ATTEMPT_TIMEOUT_MS = 6000L
        private const val BACKOFF_MS = 600L
        private const val READ_BUFFER = 1024
        private const val SEND_ATTEMPTS = 3
        private const val ACK_TIMEOUT_MS = 1000L
        const val REPLY_TIMEOUT_MS = 1500L
    }
}
