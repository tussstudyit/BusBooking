package com.example.busbooking.domain.repository

import com.example.busbooking.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class VnpayPaymentPayload(
    val paymentId: String,
    val paymentUrl: String,
    val qrContent: String,
    val qrImageBase64: String,
    val qrMimeType: String,
    val amount: Double,
    val expiresAt: Long
)

class VnpayRepository(
    private val baseUrl: String = BuildConfig.ADMIN_WEB_BASE_URL
) {
    suspend fun createPaymentUrl(paymentId: String): Result<String> {
        return createPaymentPayload(paymentId).map { it.paymentUrl }
    }

    suspend fun createPaymentPayload(paymentId: String): Result<VnpayPaymentPayload> = withContext(Dispatchers.IO) {
        try {
            val encodedPaymentId = URLEncoder.encode(paymentId, Charsets.UTF_8.name())
            val url = URL("${baseUrl.trimEnd('/')}/api/payments/vnpay/create?paymentId=$encodedPaymentId")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 15_000
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
                return@withContext Result.failure(IllegalStateException(readErrorMessage(responseText)))
            }

            val json = JSONObject(responseText)
            val code = json.optString("code")
            val paymentUrl = json.optString("paymentUrl")
            val qrImageBase64 = json.optString("qrImageBase64")
            if (code == "00" && paymentUrl.isNotBlank() && qrImageBase64.isNotBlank()) {
                Result.success(
                    VnpayPaymentPayload(
                        paymentId = json.optString("paymentId").ifBlank { paymentId },
                        paymentUrl = paymentUrl,
                        qrContent = json.optString("qrContent").ifBlank { paymentUrl },
                        qrImageBase64 = qrImageBase64,
                        qrMimeType = json.optString("qrMimeType").ifBlank { "image/png" },
                        amount = json.optDouble("amount", 0.0),
                        expiresAt = json.optLong("expiresAt", 0L)
                    )
                )
            } else {
                Result.failure(IllegalStateException(json.optString("message", "Could not create VNPAY QR")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readErrorMessage(responseText: String): String {
        if (responseText.isBlank()) {
            return "Không thể tạo link thanh toán VNPAY"
        }
        return runCatching {
            val json = JSONObject(responseText)
            json.optString("message")
                .ifBlank { json.optString("error") }
                .ifBlank { responseText }
        }.getOrElse { responseText }
    }
}
