package com.example.busbooking.domain.repository

import com.example.busbooking.BuildConfig

internal object AdminWebConfig {
    val baseUrls: List<String> by lazy {
        BuildConfig.ADMIN_WEB_BASE_URLS
            .split(',')
            .map { it.trim().trimEnd('/') }
            .filter { it.isNotBlank() }
            .distinct()
            .ifEmpty { listOf(BuildConfig.ADMIN_WEB_BASE_URL.trimEnd('/')) }
    }
}
