package com.medemini.ai.native

import android.content.Context
import android.provider.Settings

class DeviceIdGenerator {

    companion object {

        fun getDeviceId(context: Context): String {
            return try {
                val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                if (androidId != null && androidId.isNotEmpty()) {
                    androidId
                } else {
                    generateFallbackId(context)
                }
            } catch (_: Exception) {
                generateFallbackId(context)
            }
        }

        private fun generateFallbackId(context: Context): String {
            val packageName = context.packageName
            val timestamp = System.currentTimeMillis()
            val hash = (packageName.hashCode() * 31 + timestamp.hashCode()).toString(16)
            return "fallback_${hash}"
        }
    }
}