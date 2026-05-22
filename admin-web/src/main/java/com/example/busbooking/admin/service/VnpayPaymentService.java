package com.example.busbooking.admin.service;

import com.example.busbooking.admin.config.VnpayProperties;
import com.example.busbooking.admin.model.VnpayCreatePaymentResponse;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import jakarta.servlet.http.HttpServletRequest;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VnpayPaymentService {
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final Firestore firestore;
    private final VnpayProperties properties;

    public VnpayPaymentService(Firestore firestore, VnpayProperties properties) {
        this.firestore = firestore;
        this.properties = properties;
    }

    public VnpayCreatePaymentResponse createPaymentUrl(String paymentId, HttpServletRequest request) {
        validateConfig();
        try {
            DocumentReference paymentRef = firestore.collection("payments").document(paymentId);
            DocumentSnapshot payment = paymentRef.get().get();
            if (!payment.exists()) {
                throw new IllegalArgumentException("Payment not found");
            }

            Double amount = FirestoreMapper.doubleValue(payment, "amount");
            if (amount == null || amount <= 0) {
                throw new IllegalArgumentException("Invalid payment amount");
            }

            LocalDateTime now = LocalDateTime.now(VN_ZONE);
            LocalDateTime expiresAtDate = now.plusMinutes(15);
            String createDate = now.format(VNPAY_DATE);
            String expireDate = expiresAtDate.format(VNPAY_DATE);
            long expiresAtMillis = expiresAtDate.atZone(VN_ZONE).toInstant().toEpochMilli();

            Map<String, String> params = new TreeMap<>();
            params.put("vnp_Version", "2.1.0");
            params.put("vnp_Command", "pay");
            params.put("vnp_TmnCode", properties.tmnCode());
            params.put("vnp_Amount", toVnpayAmount(amount));
            params.put("vnp_CurrCode", "VND");
            params.put("vnp_TxnRef", paymentId);
            params.put("vnp_OrderInfo", "Thanh toan ve xe BusBooking " + paymentId);
            params.put("vnp_OrderType", "billpayment");
            params.put("vnp_Locale", "vn");
            params.put("vnp_ReturnUrl", properties.returnUrl());
            params.put("vnp_IpAddr", clientIp(request));
            params.put("vnp_CreateDate", createDate);
            params.put("vnp_ExpireDate", expireDate);

            String query = buildQuery(params);
            String secureHash = hmacSha512(properties.hashSecret(), buildHashData(params));
            String paymentUrl = properties.payUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
            String qrImageBase64 = createQrPngBase64(paymentUrl);

            paymentRef.update(Map.of(
                    "status", "PENDING",
                    "vnpTxnRef", paymentId,
                    "vnpCreateDate", createDate,
                    "vnpExpireDate", expireDate,
                    "expiresAt", expiresAtMillis,
                    "paymentUrl", paymentUrl,
                    "qrContent", paymentUrl,
                    "updatedAt", System.currentTimeMillis()
            )).get();

            return new VnpayCreatePaymentResponse(
                    "00",
                    "success",
                    paymentId,
                    paymentUrl,
                    paymentUrl,
                    qrImageBase64,
                    "image/png",
                    amount,
                    expiresAtMillis
            );
        } catch (Exception e) {
            throw new IllegalStateException("Could not create VNPAY payment URL", e);
        }
    }

    public Map<String, String> handleCallback(Map<String, String> params) {
        try {
            if (!verifySecureHash(params)) {
                return Map.of("RspCode", "97", "Message", "Invalid signature");
            }

            String paymentId = params.get("vnp_TxnRef");
            if (!StringUtils.hasText(paymentId)) {
                return Map.of("RspCode", "01", "Message", "Missing order reference");
            }
            DocumentReference paymentRef = firestore.collection("payments").document(paymentId);
            DocumentSnapshot payment = paymentRef.get().get();
            if (!payment.exists()) {
                return Map.of("RspCode", "01", "Message", "Order not found");
            }
            if (!validCallbackAmount(payment, params)) {
                return Map.of("RspCode", "04", "Message", "Invalid amount");
            }
            String currentStatus = payment.getString("status");
            if ("SUCCESS".equals(currentStatus)) {
                return Map.of("RspCode", "00", "Message", "Confirm success");
            }
            if ("FAILED".equals(currentStatus)) {
                return Map.of("RspCode", "00", "Message", "Payment already failed");
            }

            String responseCode = params.get("vnp_ResponseCode");
            String transactionStatus = params.get("vnp_TransactionStatus");
            boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);

            if (success) {
                confirmPayment(paymentRef, payment, params);
                return Map.of("RspCode", "00", "Message", "Confirm success");
            }

            failPayment(paymentRef, payment, params);
            return Map.of("RspCode", "00", "Message", "Payment failed");
        } catch (Exception e) {
            return Map.of("RspCode", "99", "Message", "Unknown error");
        }
    }

    public boolean verifySecureHash(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (!StringUtils.hasText(receivedHash)) {
            return false;
        }

        Map<String, String> signedParams = new TreeMap<>();
        params.forEach((key, value) -> {
            if (value != null
                    && !key.equals("vnp_SecureHash")
                    && !key.equals("vnp_SecureHashType")) {
                signedParams.put(key, value);
            }
        });

        String calculatedHash = hmacSha512(properties.hashSecret(), buildHashData(signedParams));
        return calculatedHash.equalsIgnoreCase(receivedHash);
    }

    private void confirmPayment(DocumentReference paymentRef, DocumentSnapshot payment, Map<String, String> params) throws Exception {
        WriteBatch batch = firestore.batch();
        long now = System.currentTimeMillis();
        batch.update(paymentRef, callbackPaymentUpdates(params, "SUCCESS", now));
        String paymentId = payment.getId();

        for (String ticketDocumentId : stringList(payment.get("ticketDocumentIds"))) {
            DocumentReference ticketRef = firestore.collection("tickets").document(ticketDocumentId);
            DocumentSnapshot ticket = ticketRef.get().get();
            batch.update(ticketRef, Map.of(
                    "status", "CONFIRMED",
                    "updatedAt", now
            ));

            Long tripId = FirestoreMapper.longValue(ticket, "tripId");
            Long seatId = FirestoreMapper.longValue(ticket, "seatId");
            if (tripId != null && seatId != null) {
                Map<String, Object> tripSeat = new HashMap<>();
                tripSeat.put("tripId", tripId);
                tripSeat.put("seatId", seatId);
                tripSeat.put("ticketId", FirestoreMapper.longValue(ticket, "id"));
                tripSeat.put("ticketDocumentId", ticketDocumentId);
                tripSeat.put("paymentId", paymentId);
                tripSeat.put("userId", ticket.getString("userId"));
                tripSeat.put("userNumericId", FirestoreMapper.longValue(ticket, "userNumericId"));
                tripSeat.put("status", "CONFIRMED");
                tripSeat.put("createdAt", now);
                tripSeat.put("updatedAt", now);
                batch.set(firestore.collection("tripSeats").document(tripId + "_" + seatId), tripSeat);
            }
        }

        batch.commit().get();
    }

    private void failPayment(DocumentReference paymentRef, DocumentSnapshot payment, Map<String, String> params) throws Exception {
        WriteBatch batch = firestore.batch();
        long now = System.currentTimeMillis();
        batch.update(paymentRef, callbackPaymentUpdates(params, "FAILED", now));

        for (String ticketDocumentId : stringList(payment.get("ticketDocumentIds"))) {
            batch.update(firestore.collection("tickets").document(ticketDocumentId), Map.of(
                    "status", "PAYMENT_FAILED",
                    "updatedAt", now
            ));
        }

        batch.commit().get();
    }

    private Map<String, Object> callbackPaymentUpdates(Map<String, String> params, String status, long now) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("vnpResponseCode", params.get("vnp_ResponseCode"));
        updates.put("vnpTransactionStatus", params.get("vnp_TransactionStatus"));
        updates.put("vnpTransactionNo", params.get("vnp_TransactionNo"));
        updates.put("vnpBankCode", params.get("vnp_BankCode"));
        updates.put("vnpPayDate", params.get("vnp_PayDate"));
        updates.put("updatedAt", now);
        return updates;
    }

    private boolean validCallbackAmount(DocumentSnapshot payment, Map<String, String> params) {
        Double amount = FirestoreMapper.doubleValue(payment, "amount");
        String callbackAmount = params.get("vnp_Amount");
        return amount != null
                && StringUtils.hasText(callbackAmount)
                && toVnpayAmount(amount).equals(callbackAmount);
    }

    private String toVnpayAmount(Double amount) {
        return BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private String buildQuery(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getValue()))
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String buildHashData(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getValue()))
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getKey() + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    private String hmacSha512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Could not sign VNPAY data", e);
        }
    }

    private String createQrPngBase64(String content) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);

        BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 720, 720, hints);
        BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
        int black = Color.BLACK.getRGB();
        int white = Color.WHITE.getRGB();
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                image.setRGB(x, y, matrix.get(x, y) ? black : white);
            }
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Could not create VNPAY QR code", e);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : rawList) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.payUrl())
                || !StringUtils.hasText(properties.tmnCode())
                || !StringUtils.hasText(properties.hashSecret())
                || !StringUtils.hasText(properties.returnUrl())) {
            throw new IllegalStateException("Missing VNPAY configuration");
        }
    }
}
