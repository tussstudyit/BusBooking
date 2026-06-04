package com.example.busbooking.domain.repository

internal object AdminWebConfig {
    val baseUrls: List<String>
        get() = ServerConfig.baseUrls()
}
