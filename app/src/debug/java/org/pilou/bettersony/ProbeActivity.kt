package org.pilou.bettersony

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.pilou.bettersony.mdr.MdrClient
import org.pilou.bettersony.mdr.MsgType
import org.pilou.bettersony.mdr.hex
import org.pilou.bettersony.Probe.Companion.TAG
import java.io.File

/**
 * Raw MDR probe, debug builds only. Runs read-only GETs by default.
 * Extras: `gets` (";"-separated hex payloads replacing the default list),
 * `sends` (";"-separated hex payloads, "2:" prefix for table 2), `listen` (seconds).
 */
/** Headless probe: `am broadcast -n org.pilou.bettersony/.ProbeReceiver --es gets ...`, output in logcat tag MDR. */
@SuppressLint("MissingPermission")
class ProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val adapter = context.getSystemService(BluetoothManager::class.java).adapter
        val device = adapter.bondedDevices.firstOrNull { it.name?.startsWith("WH-") == true }
        val extras = Intent(intent)
        val file = File(context.getExternalFilesDir(null), "probe.txt").apply { writeText("") }
        val log: (String) -> Unit = { Log.i(Probe.TAG, it); file.appendText(it + "\n") }
        Thread({
            runCatching { device?.let { Probe(extras, log).run(it) } ?: log("no headset") }
                .onFailure { log("FAILED: ${it.message} / ${it.cause?.message}") }
            log("== END")
        }, "probe").start()
    }
}

@SuppressLint("MissingPermission", "SetTextI18n")
class ProbeActivity : Activity() {
    private lateinit var output: TextView
    private lateinit var scroll: ScrollView
    private val transcript = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        output = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 10f
            setPadding(24, 24, 24, 24)
            setTextIsSelectable(true)
        }
        scroll = ScrollView(this).apply { addView(output) }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
            addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        })
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
        } else {
            start()
        }
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        if (results.firstOrNull() == PackageManager.PERMISSION_GRANTED) start() else log("permission denied")
    }

    private fun log(line: String) {
        Log.i(Probe.TAG, line)
        synchronized(transcript) { transcript.append(line).append('\n') }
        runOnUiThread {
            output.append(line + "\n")
            scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private fun start() {
        val adapter = getSystemService(BluetoothManager::class.java).adapter
        adapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                val connected = (proxy as BluetoothA2dp).connectedDevices
                adapter.closeProfileProxy(BluetoothProfile.A2DP, proxy)
                val device = connected.firstOrNull { it.name?.startsWith("WH-") == true } ?: connected.firstOrNull()
                Thread({
                    try {
                        if (device == null) log("no connected headset") else Probe(intent, ::log).run(device)
                    } catch (e: Exception) {
                        log("FAILED: ${e.javaClass.simpleName}: ${e.message} / ${e.cause?.message}")
                    } finally {
                        getExternalFilesDir(null)?.let { File(it, "probe.txt").writeText(transcript.toString()) }
                        log("== END")
                    }
                }, "probe").start()
            }
            override fun onServiceDisconnected(profile: Int) = Unit
        }, BluetoothProfile.A2DP)
    }

}

/** Shared probe logic, logging to logcat tag MDR. */
@SuppressLint("MissingPermission")
class Probe(private val intent: Intent, private val log: (String) -> Unit = { Log.i(TAG, it) }) {
    private fun parse(spec: String): Pair<Int, ByteArray> {
        val table2 = spec.startsWith("2:")
        val hex = spec.removePrefix("2:").replace(" ", "")
        val bytes = ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        return (if (table2) MsgType.TABLE_2 else MsgType.TABLE_1) to bytes
    }

    fun run(device: BluetoothDevice) {
        log("target: ${device.name} ${device.address}")
        log("uuids: ${device.uuids?.joinToString()}")
        val t0 = System.currentTimeMillis()
        val client = MdrClient(device, log)
        client.onUnsolicited = { log("   (notify) $it") }
        client.connect()
        log("connect ${System.currentTimeMillis() - t0} ms")
        val init = client.request(byteArrayOf(0, 0), reply = 1)
        log("== init: ${init?.payload?.hex()}")

        val gets = intent.getStringExtra("gets")?.split(';')?.filter { it.isNotBlank() } ?: DEFAULT_GETS
        for (spec in gets) {
            val (type, payload) = parse(spec)
            val r = runCatching { client.request(payload, type = type) }
            log("== GET ${spec}: ${r.getOrNull()?.payload?.let { it.hex() + "  |" + ascii(it) } ?: r.exceptionOrNull()?.message ?: "TIMEOUT"}")
        }
        intent.getStringExtra("sends")?.split(';')?.filter { it.isNotBlank() }?.forEach { spec ->
            val (type, payload) = parse(spec)
            runCatching { client.send(payload, type) }.onFailure { log("send failed: ${it.message}") }
            log("== SENT $spec")
            while (true) { val f = client.nextFrame(NOTIFY_WAIT_MS) ?: break; log("   (after) $f") }
        }
        val listen = intent.getIntExtra("listen", 0)
        if (listen > 0) {
            log("== listening ${listen}s")
            val end = System.currentTimeMillis() + listen * 1000L
            while (System.currentTimeMillis() < end) {
                client.nextFrame(end - System.currentTimeMillis())?.let { log("   (live) $it  |${ascii(it.payload)}") }
            }
        }
        client.close()
        log("done in ${System.currentTimeMillis() - t0} ms")
    }

    private fun ascii(b: ByteArray) = String(CharArray(b.size) { val c = b[it].toInt() and 0xFF; if (c in 32..126) c.toChar() else '.' })

    companion object {
        const val TAG = "MDR"
        private const val NOTIFY_WAIT_MS = 1200L
        private val DEFAULT_GETS = listOf(
            "0200", "0401", "0402", "0403", "0404", "0600",
            "1000", "1400", "1800",
            "6002", "6202", "6602", "8001", "8601",
            "5001 01", "5601", "5a01",
            "e001", "e002", "e601", "e602",
            "f001", "f002", "f003", "f004", "f005", "f006",
            "f601", "f602", "f603", "f604", "f605", "f606",
            "fa03", "fa05",
            "d0d1 01", "d0d2 01", "d0d3 01", "d6d1", "d6d2", "d6d3",
            "a001", "a601 20", "4001", "4002", "4601", "4602", "7001",
            "2:4001", "2:4601 01", "2:4601 02", "2:3001", "2:3601",
        )
    }
}
