package com.example.busbooking.staff.data.api

import com.example.busbooking.staff.data.model.ApiMessage
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object StaffApiClient {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    internal suspend inline fun <reified T> get(path: String): T = request("GET", path, null)

    internal suspend inline fun <reified Req, reified Res> post(path: String, body: Req): Res {
        return request("POST", path, json.encodeToString(body))
    }

    internal suspend inline fun <reified T> request(method: String, path: String, body: String?): T {
        return withContext(Dispatchers.IO) {
            val baseUrls = StaffServerConfig.baseUrls()
            var lastFailure: Throwable? = null
            baseUrls.forEachIndexed { index, baseUrl ->
                try {
                    return@withContext executeRequest<T>(baseUrl, method, path, body)
                } catch (e: Exception) {
                    lastFailure = e
                    if (e !is IOException || index == baseUrls.lastIndex) {
                        throw e
                    }
                }
            }
            throw lastFailure ?: IOException("Khong the ket noi Spring web")
        }
    }

    @PublishedApi
    internal inline fun <reified T> executeRequest(baseUrl: String, method: String, path: String, body: String?): T {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
        }
        if (body != null) {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.let {
            BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use(BufferedReader::readText)
        }.orEmpty()
        connection.disconnect()
        if (code !in 200..299) {
            throw IllegalStateException(parseError(text).ifBlank { "HTTP $code" })
        }
        return if (T::class == Unit::class) Unit as T else json.decodeFromString<T>(text)
    }

    fun parseError(text: String): String {
        return runCatching { json.decodeFromString<ApiMessage>(text).message }.getOrNull().orEmpty()
    }
}
