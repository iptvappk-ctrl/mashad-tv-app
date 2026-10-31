package com.madar.tv.core

/**
 * Decodes legacy, statically encoded strings used by existing repositories.
 *
 * This is reversible string obfuscation, not cryptographic encryption: the
 * encoded bytes and decoder are both shipped in the APK. Do not put credentials
 * or other secrets here. Runtime behavior and the existing encoded payload
 * format are retained for compatibility.
 */
internal object X {
    private val key1: Int = (0x2E shl 1)
    private val key2: Int = (0x51 shl 1) or 1
    private val key3: Int = (0x3B xor 0x04)
    private val rotation: Int = (9 - 6)
    private val shift: Int = (34 ushr 1)
    private val crcPolynomial: Int = 0x07

    private fun rotateRight(value: Int, bits: Int): Int {
        val amount = bits and 7
        return ((value ushr amount) or (value shl (8 - amount))) and 0xFF
    }

    private fun swapNibbles(value: Int): Int =
        ((value shr 4) and 0x0F) or ((value and 0x0F) shl 4)

    private fun crc8(data: ByteArray): Int {
        var crc = 0xFF
        for (byte in data) {
            crc = crc xor (byte.toInt() and 0xFF)
            repeat(8) {
                crc = if (crc and 0x80 != 0) {
                    ((crc shl 1) xor crcPolynomial) and 0xFF
                } else {
                    (crc shl 1) and 0xFF
                }
            }
        }
        return crc
    }

    /** Decodes bytes encoded with the legacy fixed-transform string format. */
    fun s(bytes: ByteArray): String {
        val decoded = ByteArray(bytes.size)
        for (index in bytes.indices) {
            var value = bytes[index].toInt() and 0xFF
            value = value xor ((key3 xor ((index * 7 + 13) and 0xFF)) and 0xFF)
            value = swapNibbles(value)
            value = (value - shift) and 0xFF
            value = value xor key2
            value = rotateRight(value, rotation)
            value = value xor key1
            decoded[index] = value.toByte()
        }
        return String(decoded, Charsets.UTF_8)
    }

    /**
     * Decodes the legacy payload format with a trailing CRC-8 byte.
     * CRC-8 only detects some accidental corruption; it is not a secure MAC.
     */
    fun sv(bytes: ByteArray): String {
        if (bytes.size < 2) return ""
        val payload = bytes.copyOfRange(0, bytes.size - 1)
        val expectedCrc = bytes.last().toInt() and 0xFF
        if (crc8(payload) != expectedCrc) return ""
        return s(payload)
    }

    /** Joins legacy obfuscated string segments. */
    fun j(vararg parts: ByteArray): String = parts.joinToString("") { s(it) }

    /** Joins segments that use the legacy CRC-8 wrapper. */
    fun jv(vararg parts: ByteArray): String = parts.joinToString("") { sv(it) }
}
