package com.example.busbooking.staff.data.api

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.example.busbooking.staff.BuildConfig
import java.util.Locale

internal object StaffServerConfig {
    private const val PREFS_NAME = "busbooking_staff_server_config"
    private const val KEY_BASE_URL = "base_url"

    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setBaseUrl(value: String) {
        val normalized = normalize(value)
        if (normalized.isBlank()) return
        prefs?.edit()?.putString(KEY_BASE_URL, normalized)?.apply()
    }

    fun primaryBaseUrl(): String = baseUrls().first()

    fun baseUrls(): List<String> {
        val savedUrl = prefs?.getString(KEY_BASE_URL, null)
        val configuredUrls = BuildConfig.API_BASE_URLS
            .split(',')
            .plus(BuildConfig.API_BASE_URL)

        return listOfNotNull(savedUrl)
            .plus(configuredUrls)
            .map { normalize(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .prioritizeForDevice()
    }

    private fun normalize(value: String): String {
        val trimmed = value.trim().trimEnd('/')
        if (trimmed.isBlank()) return ""
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
    }

    private fun List<String>.prioritizeForDevice(): List<String> {
        if (!isProbablyEmulator()) return this
        val (emulatorUrls, otherUrls) = partition { it.isEmulatorLoopbackUrl() }
        return (emulatorUrls + otherUrls).distinct()
    }

    private fun String.isEmulatorLoopbackUrl(): Boolean {
        val value = lowercase(Locale.US)
        return value.contains("://10.0.2.2") ||
            value.contains("://localhost") ||
            value.contains("://127.0.0.1")
    }

    private fun isProbablyEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase(Locale.US)
        val model = Build.MODEL.lowercase(Locale.US)
        val product = Build.PRODUCT.lowercase(Locale.US)
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.US)
        val brand = Build.BRAND.lowercase(Locale.US)
        val device = Build.DEVICE.lowercase(Locale.US)
        val hardware = Build.HARDWARE.lowercase(Locale.US)

        return fingerprint.startsWith("generic") ||
            fingerprint.contains("emulator") ||
            model.contains("sdk") ||
            model.contains("emulator") ||
            model.contains("android sdk built for") ||
            product.contains("sdk") ||
            manufacturer.contains("genymotion") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu") ||
            brand.startsWith("generic") && device.startsWith("generic")
    }
}
