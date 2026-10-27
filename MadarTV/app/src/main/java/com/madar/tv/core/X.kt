package com.madar.tv.core

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║            Runtime String Protection Engine — v6  (6-Layer)                ║
 * ║                                                                            ║
 * ║  6-Layer encryption pipeline (applied in REVERSE at runtime):              ║
 * ║    Enc: XOR(K1) → ROT-L(3) → XOR(K2) → Caesar(+17)                       ║
 * ║         → Nibble-Swap → XOR(K3,position-dependent)                        ║
 * ║    Dec: XOR(K3,pos) → Nibble-Swap → Caesar(-17) → XOR(K2)                ║
 * ║         → ROT-R(3) → XOR(K1)                                             ║
 * ║                                                                            ║
 * ║  + Integrity verification (CRC8)                                           ║
 * ║  + Anti-debug detection                                                    ║
 * ║  + Decoy methods & fields to mislead reverse engineers                     ║
 * ║  + Position-dependent key mixing for diffusion                             ║
 * ║                                                                            ║
 * ║  After R8 obfuscation: class → single letter, methods inlined,             ║
 * ║  all names randomised — requires ALL 6 parameters + position knowledge     ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
internal object X {

    // ── Keys (obscured via computed expressions, NOT plain literals) ──────────
    private val k1: Int = (0x2E shl 1)            // = 0x5C
    private val k2: Int = (0x51 shl 1) or 1        // = 0xA3
    private val k3: Int = (0x3B xor 0x04)          // = 0x3F — new layer-5/6 key
    private val rot: Int = (9 - 6)                 // = 3
    private val shf: Int = (34 ushr 1)             // = 17

    // ── Integrity seed (CRC8 polynomial 0x07) ──────────────────────────────
    private val crcPoly: Int = 0x07

    // ── Fake / decoy constants to confuse decompilers ─────────────────────────
    @Suppress("unused") private val _d1 = byteArrayOf(42, 13, 7, 99, 0, 55, 192.toByte())
    @Suppress("unused") private val _d2 = byteArrayOf(77, 200.toByte(), 33, 11, 144.toByte())
    @Suppress("unused") private val _d3 = "v6.0.0-fortified"
    @Suppress("unused") private val _d4 = intArrayOf(0xDE, 0xAD, 0xBE, 0xEF, 0xCA, 0xFE)
    @Suppress("unused") private val _d5 = longArrayOf(0x1337DEADBEEF, 0xCAFEBABE)
    @Suppress("unused") private val _d6 = byteArrayOf(0x7F, 0x45, 0x4C, 0x46) // ELF header decoy

    // ── Anti-debug: detect debugger attachment ──────────────────────────────
    private val _safe: Boolean by lazy {
        try {
            // Check if debugger is connected
            val rt = Class.forName("android.os.Debug")
            val m = rt.getMethod("isDebuggerConnected")
            !(m.invoke(null) as Boolean)
        } catch (_: Throwable) { true }
    }

    // ── Core pipeline functions ──────────────────────────────────────────────

    /** Rotate byte RIGHT by [n] bits (reverse of left rotation) */
    private fun rr(b: Int, n: Int): Int {
        val sh = n and 7
        return ((b ushr sh) or (b shl (8 - sh))) and 0xFF
    }

    /** Swap high and low nibbles of a byte */
    private fun ns(b: Int): Int = ((b shr 4) and 0x0F) or ((b and 0x0F) shl 4)

    /** CRC8 integrity check */
    private fun crc8(data: ByteArray): Int {
        var crc = 0xFF
        for (b in data) {
            crc = crc xor (b.toInt() and 0xFF)
            for (bit in 0 until 8) {
                crc = if (crc and 0x80 != 0) ((crc shl 1) xor crcPoly) and 0xFF
                      else (crc shl 1) and 0xFF
            }
        }
        return crc
    }

    /**
     * Decrypt a 6-layer encrypted byte array back to a String.
     * Pipeline (reversed from encryption):
     *   1. XOR with K3 (position-dependent: K3 XOR (position * 7 + 13))
     *   2. Nibble swap
     *   3. Subtract Caesar shift (-17)
     *   4. XOR with K2
     *   5. Rotate right by 3 bits
     *   6. XOR with K1
     *
     * If debugger is attached, returns garbage to foil dynamic analysis.
     */
    fun s(b: ByteArray): String {
        if (!_safe) return _d3 // return decoy if debugged

        val r = ByteArray(b.size)
        for (i in b.indices) {
            var v = b[i].toInt() and 0xFF
            // Layer 6 reverse: position-dependent XOR with K3
            v = v xor ((k3 xor ((i * 7 + 13) and 0xFF)) and 0xFF)
            // Layer 5 reverse: nibble swap
            v = ns(v)
            // Layer 4 reverse: Caesar shift
            v = (v - shf) and 0xFF
            // Layer 3 reverse: XOR with K2
            v = v xor k2
            // Layer 2 reverse: rotate right
            v = rr(v, rot)
            // Layer 1 reverse: XOR with K1
            v = v xor k1
            r[i] = v.toByte()
        }
        return String(r, Charsets.UTF_8)
    }

    /**
     * Decrypt with CRC8 integrity verification.
     * Last byte of input is the expected CRC8. If mismatch → returns empty.
     * Prevents tampering with encrypted payloads.
     */
    fun sv(b: ByteArray): String {
        if (b.size < 2) return ""
        val payload = b.copyOfRange(0, b.size - 1)
        val expected = b[b.size - 1].toInt() and 0xFF
        if (crc8(payload) != expected) return "" // integrity fail
        return s(payload)
    }

    // ── Segment joiner (for split-string obfuscation) ─────────────────────────
    /**
     * Joins multiple decrypted segments — used to reassemble URLs that are
     * stored as separate encrypted chunks to defeat pattern matching.
     */
    fun j(vararg parts: ByteArray): String = parts.joinToString("") { s(it) }

    /**
     * Joins with CRC8 verification on each segment.
     */
    fun jv(vararg parts: ByteArray): String = parts.joinToString("") { sv(it) }

    // ── Anti-analysis: honeypot trap functions ──────────────────────────────
    /** Intentionally misleading function — returns garbage data */
    @Suppress("unused")
    private fun trap1(x: Int): Int = ((x * 0x9E3779B9.toInt()) ushr 16) and 0xFF

    @Suppress("unused")
    private fun trap2(b: ByteArray): ByteArray = b.reversedArray()

    @Suppress("unused")
    private fun trap3(b: ByteArray): String {
        val sb = StringBuilder()
        for (i in b.indices) sb.append(('a' + (b[i].toInt() and 0x0F)).toChar())
        return sb.toString()
    }

    @Suppress("unused")
    private fun trap4(key: Int, data: ByteArray): ByteArray {
        // AES-like confusion — actually just noise
        return ByteArray(data.size) { ((data[it].toInt() xor (key * (it + 1))) and 0xFF).toByte() }
    }

    // ── Decoy encrypted data arrays to waste reverse engineer time ──────────
    @Suppress("unused") private val _honeypot1 = byteArrayOf(
        0x48, 0x2A, 0x71.toByte(), 0x93.toByte(), 0xF4.toByte(), 0x1B, 0xDE.toByte(), 0x67,
        0x89.toByte(), 0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte(), 0x01, 0x23, 0x45, 0x67
    )
    @Suppress("unused") private val _honeypot2 = byteArrayOf(
        0xFE.toByte(), 0xDC.toByte(), 0xBA.toByte(), 0x98.toByte(), 0x76, 0x54, 0x32, 0x10,
        0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88.toByte()
    )
}
