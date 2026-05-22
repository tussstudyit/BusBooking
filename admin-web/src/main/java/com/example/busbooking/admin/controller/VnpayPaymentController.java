package com.example.busbooking.admin.controller;

import com.example.busbooking.admin.model.VnpayCreatePaymentResponse;
import com.example.busbooking.admin.service.VnpayPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VnpayPaymentController {
    private final VnpayPaymentService vnpayPaymentService;

    public VnpayPaymentController(VnpayPaymentService vnpayPaymentService) {
        this.vnpayPaymentService = vnpayPaymentService;
    }

    @PostMapping("/api/payments/vnpay/create")
    public VnpayCreatePaymentResponse create(
            @RequestParam String paymentId,
            HttpServletRequest request
    ) {
        try {
            return vnpayPaymentService.createPaymentUrl(paymentId, request);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return new VnpayCreatePaymentResponse("99", rootMessage(e), null);
        }
    }

    @GetMapping("/api/payments/vnpay/ipn")
    public Map<String, String> ipn(@RequestParam Map<String, String> params) {
        return vnpayPaymentService.handleCallback(params);
    }

    @GetMapping("/api/payments/vnpay/return")
    public ResponseEntity<String> vnpayReturn(@RequestParam Map<String, String> params) {
        Map<String, String> result = vnpayPaymentService.handleCallback(params);
        boolean handled = "00".equals(result.get("RspCode"));
        String paymentId = params.getOrDefault("vnp_TxnRef", "");
        String appReturnUrl = "busbooking://payment-return?paymentId=" + encode(paymentId);
        String message = handled
                ? "Thanh toan VNPAY da duoc xu ly. Ban co the quay lai ung dung BusBooking."
                : "Thanh toan VNPAY khong hop le: " + result.get("Message");
        return ResponseEntity.ok("""
                <!doctype html>
                <html lang="vi">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>BusBooking VNPAY</title>
                </head>
                <body style="font-family:Arial,sans-serif;padding:32px">
                  <h2>%s</h2>
                  <p><a href="%s" style="display:inline-block;margin-top:16px;padding:12px 18px;background:#1565c0;color:white;text-decoration:none;border-radius:6px">Mo lai ung dung</a></p>
                  <script>
                    setTimeout(function () { window.location.href = "%s"; }, 1200);
                  </script>
                </body>
                </html>
                """.formatted(escapeHtml(message), appReturnUrl, appReturnUrl));
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? throwable.getMessage() : current.getMessage();
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
