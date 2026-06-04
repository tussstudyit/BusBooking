package com.example.busbooking.utils

import java.util.Locale

object StatusLabels {
    fun ticket(status: String): String = when (status.uppercase(Locale.ROOT)) {
        "CONFIRMED" -> "Đã xác nhận"
        "CHECKED_IN" -> "Đã lên xe"
        "PENDING", "PENDING_PAYMENT", "CREATED" -> "Chờ thanh toán"
        "SUCCESS" -> "Thanh toán thành công"
        "FAILED", "PAYMENT_FAILED" -> "Thanh toán thất bại"
        "EXPIRED" -> "Hết hạn thanh toán"
        "CANCELLED" -> "Đã hủy"
        "COMPLETED", "USED" -> "Đã đi"
        else -> "Chưa xác định"
    }

    fun trip(status: String): String = when (status.uppercase(Locale.ROOT)) {
        "SCHEDULED" -> "Chưa khởi hành"
        "DEPARTED", "RUNNING" -> "Đang chạy"
        "COMPLETED" -> "Hoàn thành"
        "CANCELLED" -> "Đã hủy"
        else -> "Chưa xác định"
    }
}
