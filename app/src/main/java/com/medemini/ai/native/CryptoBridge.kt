package com.medemini.ai.native

class CryptoBridge {

    companion object {

        private val ENCRYPTED_FRAGMENTS = listOf(
            intArrayOf(0x29, 0x50, 0x51, 0x54, 0xAE, 0xD4, 0x3C, 0x78, 0x1F, 0x5B, 0xCB, 0x96, 0x69, 0x5B, 0x3F, 0x27),
            intArrayOf(0xD6, 0x86, 0xBF, 0xC4, 0x4A, 0x4E, 0x22, 0x33, 0xEB, 0xCA, 0xA4, 0xD9, 0x46, 0x04, 0x33, 0x1A),
            intArrayOf(0xDC, 0xD7, 0xB1, 0x90, 0x78, 0x59, 0x1A, 0x09, 0xDF, 0xFC, 0x8B, 0xAD, 0x44, 0x5C, 0x0A, 0x17),
            intArrayOf(0xC7, 0xF6, 0xEF)
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
            ENCRYPTED_FRAGMENTS.forEach { fragment ->
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