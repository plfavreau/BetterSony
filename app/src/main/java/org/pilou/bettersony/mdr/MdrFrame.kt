package org.pilou.bettersony.mdr

/** Message types carried in the second byte of every MDR frame. */
object MsgType {
    const val ACK = 0x01
    const val TABLE_1 = 0x0C
    const val TABLE_2 = 0x0E
}

/**
 * One MDR message: `3e | escape(type seq len[4] payload checksum) | 3c`.
 * The checksum is the byte sum of everything between the markers, before escaping.
 */
class MdrFrame(val type: Int, val seq: Int, val payload: ByteArray = ByteArray(0)) {
    val command: Int get() = if (payload.isEmpty()) -1 else payload[0].u8
    val inquired: Int get() = if (payload.size < 2) -1 else payload[1].u8

    fun encode(): ByteArray {
        val body = ByteArray(HEADER + payload.size)
        body[0] = type.toByte()
        body[1] = seq.toByte()
        for (i in 0 until LEN_BYTES) body[2 + i] = (payload.size ushr (8 * (LEN_BYTES - 1 - i))).toByte()
        payload.copyInto(body, HEADER)
        val out = ArrayList<Byte>(body.size + EXTRA)
        out += START
        for (b in body + checksum(body).toByte()) {
            if (b == START || b == END || b == ESCAPE) {
                out += ESCAPE
                out += (b.u8 and ESCAPE_MASK).toByte()
            } else {
                out += b
            }
        }
        out += END
        return out.toByteArray()
    }

    override fun toString(): String {
        val kind = when (type) {
            MsgType.ACK -> "ACK"
            MsgType.TABLE_1 -> "T1"
            MsgType.TABLE_2 -> "T2"
            else -> "type%02x".format(type)
        }
        return "$kind s$seq ${payload.hex()}"
    }

    companion object {
        const val START: Byte = 0x3E
        const val END: Byte = 0x3C
        const val ESCAPE: Byte = 0x3D
        private const val ESCAPE_MASK = 0xEF
        private const val UNESCAPE_BIT = 0x10
        private const val LEN_BYTES = 4
        private const val HEADER = 2 + LEN_BYTES
        private const val EXTRA = 8

        fun checksum(body: ByteArray): Int = body.sumOf { it.u8 } and 0xFF

        /** Decodes the bytes between the markers; null when malformed or the checksum is off. */
        fun decode(escaped: ByteArray): MdrFrame? {
            val raw = ArrayList<Byte>(escaped.size)
            var i = 0
            while (i < escaped.size) {
                val b = escaped[i]
                if (b == ESCAPE) {
                    if (++i >= escaped.size) return null
                    raw += (escaped[i].u8 or UNESCAPE_BIT).toByte()
                } else {
                    raw += b
                }
                i++
            }
            if (raw.size < HEADER + 1) return null
            val body = raw.subList(0, raw.size - 1).toByteArray()
            if (checksum(body) != raw.last().u8) return null
            var len = 0
            for (k in 0 until LEN_BYTES) len = (len shl 8) or body[2 + k].u8
            if (len != body.size - HEADER) return null
            return MdrFrame(body[0].u8, body[1].u8, body.copyOfRange(HEADER, body.size))
        }
    }
}

/** Splits the RFCOMM byte stream into frames; a raw 0x3c never appears inside one. */
class MdrStreamParser {
    private val buffer = ArrayList<Byte>()
    private var inFrame = false

    fun feed(bytes: ByteArray, count: Int = bytes.size): List<MdrFrame> {
        val frames = mutableListOf<MdrFrame>()
        for (k in 0 until count) {
            val b = bytes[k]
            when {
                b == MdrFrame.START -> { buffer.clear(); inFrame = true }
                b == MdrFrame.END && inFrame -> {
                    MdrFrame.decode(buffer.toByteArray())?.let { frames += it }
                    buffer.clear()
                    inFrame = false
                }
                inFrame -> buffer += b
            }
        }
        return frames
    }
}

val Byte.u8: Int get() = toInt() and 0xFF

fun ByteArray.hex(): String = joinToString(" ") { "%02x".format(it.u8) }

fun bytesOf(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }
