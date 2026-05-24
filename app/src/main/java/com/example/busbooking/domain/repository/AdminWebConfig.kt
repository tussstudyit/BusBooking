package com.example.busbooking.domain.repository

import android.os.Build
import com.example.busbooking.BuildConfig
import java.util.Locale

internal object AdminWebConfig {
    val baseUrls: List<String> by lazy {
        val configuredUrls = BuildConfig.ADMIN_WEB_BASE_URLS
            .split(',')
            .map { it.trim().trimEnd('/') }
            .filter { it.isNotBlank() }
            .distinct()
            .ifEmpty { listOf(BuildConfig.ADMIN_WEB_BASE_URL.trimEnd('/')) }
        configuredUrls.prioritizeForDevice()
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
