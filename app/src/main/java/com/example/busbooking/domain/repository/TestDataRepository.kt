package com.example.busbooking.domain.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class TestDataRepository(
    private val baseUrls: List<String> = AdminWebConfig.baseUrls
) {
    suspend fun refreshRollingTrips(): Result<Unit> = withContext(Dispatchers.IO) {
        requestWithFallback { baseUrl -> refreshRollingTrips(baseUrl) }
    }

    private fun refreshRollingTrips(baseUrl: String): Result<Unit> {
        return try {
            val url = URL("${baseUrl.trimEnd('/')}/api/test-data/refresh")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4_000
                readTimeout = 8_000
                doOutput = true
            }

            OutputStreamWriter(connection.outputStream).use { it.write("") }

            val responseCode = connection.responseCode
            val responseText = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }

            if (responseCode !in 200..299) {
                return Result.failure(IllegalStateException(responseText.ifBlank { "Không thể cập nhật dữ liệu test" }))
            }

            val json = JSONObject(responseText)
            if (json.optString("code") == "00") {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException(json.optString("message", "Không thể cập nhật dữ liệu test")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun <T> requestWithFallback(request: (String) -> Result<T>): Result<T> {
        var lastFailure: Throwable? = null
        baseUrls.forEachIndexed { index, baseUrl ->
            val result = request(baseUrl)
            if (result.isSuccess) return result

            val failure = result.exceptionOrNull()
            lastFailure = failure
            if (failure !is IOException || index == baseUrls.lastIndex) {
                return result
            }
        }
        return Result.failure(lastFailure ?: IllegalStateException("Kh\u00f4ng th\u1ec3 k\u1ebft n\u1ed1i admin-web"))
    }
}
