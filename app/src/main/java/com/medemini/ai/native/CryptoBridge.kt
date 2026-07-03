package com.medemini.ai.native

class CryptoBridge {

    companion object {

        private val KEY_FRAGMENTS = listOf(
            intArrayOf(0x73, 0x6B, 0x2D, 0x49, 0x30, 0x6B, 0x36, 0x53, 0x53, 0x36, 0x45, 0x39, 0x79, 0x6A, 0x6D, 0x54),
            intArrayOf(0x42, 0x33, 0x69, 0x33, 0x42, 0x67, 0x68, 0x58, 0x67, 0x67, 0x6A, 0x36, 0x56, 0x35, 0x61, 0x69),
            intArrayOf(0x48, 0x62, 0x67, 0x67, 0x70, 0x70, 0x50, 0x62, 0x53, 0x51, 0x45, 0x42, 0x54, 0x6D, 0x58, 0x64),
            intArrayOf(0x53, 0x43, 0x39)
        )

        private val XOR_KEY = intArrayOf(
            0x5A, 0x3B, 0x7C, 0x1D, 0x9E, 0xBF, 0x0A, 0x2B,
            0x4C, 0x6D, 0x8E, 0xAF, 0x10, 0x31, 0x52, 0x73,
            0x94, 0xB5, 0xD6, 0xF7, 0x08, 0x29, 0x4A, 0x6B,
            0x8C, 0xAD, 0xCE, 0xEF, 0x10, 0x31, 0x52, 0x73,
            0x94, 0xB5, 0xD6, 0xF7, 0x08, 0x29, 0x4A, 0x6B,
            0x8C, 0xAD, 0xCE, 0xEF, 0x10, 0x31, 0x52, 0x73,
            0x94, 0xB5, 0xD6, 0xF7, 0x08, 0x29, 0x4A, 0x6B,
            0x8C, 0xAD, 0xCE, 0xEF
        )

        fun getBuiltinKey(): String {
            val sb = StringBuilder()
            var pos = 0
            KEY_FRAGMENTS.forEach { fragment ->
                fragment.forEach { c ->
                    if (c != 0) {
                        val keyPos = pos % XOR_KEY.size
                        sb.append((c.xor(XOR_KEY[keyPos])).toChar())
                        pos++
                    }
                }
            }
            return sb.toString()
        }

        fun decrypt(encryptedData: ByteArray): String {
            val sb = StringBuilder()
            var pos = 0
            encryptedData.forEach { byte ->
                val keyPos = pos % XOR_KEY.size
                sb.append((byte.toInt().xor(XOR_KEY[keyPos])).toChar())
                pos++
            }
            return sb.toString()
        }

        fun encrypt(plainText: String): ByteArray {
            val result = ByteArray(plainText.length)
            var pos = 0
            plainText.forEachIndexed { index, c ->
                val keyPos = pos % XOR_KEY.size
                result[index] = (c.toInt().xor(XOR_KEY[keyPos])).toByte()
                pos++
            }
            return result
        }

        fun getSeed(deviceId: String): Int {
            var hash = 0x12345678
            deviceId.forEach { c ->
                hash = ((hash shl 5) + hash) + c.toInt()
            }
            return hash and 0x7FFFFFFF
        }

        fun getSignature(): String {
            val chars = "0123456789ABCDEF"
            val sb = StringBuilder()
            val seed = System.nanoTime()
            var hash = seed.toInt()
            repeat(32) {
                hash = ((hash shl 5) - hash) + it
                sb.append(chars[(hash and 0xF)])
            }
            return sb.toString()
        }
    }
}
