package com.example.busbooking.staff.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object StaffFormatters {
    private val timeFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.forLanguageTag("vi-VN")).apply {
        timeZone = TimeZone.getTimeZone("Asia/Bangkok")
    }

    fun dateTime(millis: Long): String {
        if (millis <= 0L) return "--:--"
        return timeFormat.format(Date(millis))
    }

    fun tripStatus(value: String, departureTime: Long, now: Long = System.currentTimeMillis()): String {
        val normalizedStatus = value.uppercase(Locale.ROOT)
        val effectiveStatus = if (normalizedStatus == "SCHEDULED" && departureTime > 0L && departureTime <= now) {
            "DEPARTED"
        } else {
            normalizedStatus
        }
        return tripStatus(effectiveStatus)
    }

    fun tripStatus(value: String): String = when (value.uppercase(Locale.ROOT)) {
        "SCHEDULED" -> "Chưa khởi hành"
        "DEPARTED" -> "Đã khởi hành"
        "RUNNING" -> "Đang chạy"
        "COMPLETED" -> "Hoàn thành"
        "CANCELLED" -> "Đã hủy"
        else -> "Chưa xác định"
    }

    fun paymentStatus(value: String): String = when (value.uppercase(Locale.ROOT)) {
        "SUCCESS", "CONFIRMED", "PAID" -> "Đã thanh toán"
        "PENDING", "PENDING_PAYMENT", "CREATED" -> "Chưa thanh toán"
        "FAILED", "PAYMENT_FAILED" -> "Thanh toán thất bại"
        "EXPIRED" -> "Hết hạn thanh toán"
        "CANCELLED" -> "Đã hủy"
        else -> "Chưa xác định"
    }

    fun checkInStatus(value: String): String = when (value.uppercase(Locale.ROOT)) {
        "CHECKED_IN" -> "Đã lên xe"
        else -> "Chưa lên xe"
    }

    fun seatStatus(value: String): String = when (value.uppercase(Locale.ROOT)) {
        "CHECKED_IN" -> "Đã lên xe"
        "BOOKED" -> "Đã đặt"
        "AVAILABLE" -> "Còn trống"
        else -> "Chưa xác định"
    }
}
