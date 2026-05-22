package com.example.busbooking.admin.service;

import com.example.busbooking.admin.model.PaymentDto;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PaymentAdminService {
    private static final int DEFAULT_PAYMENT_LIMIT = 200;

    private final Firestore firestore;

    public PaymentAdminService(Firestore firestore) {
        this.firestore = firestore;
    }

    public List<PaymentDto> findAll() {
        try {
            return firestore.collection("payments")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(DEFAULT_PAYMENT_LIMIT)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(this::toDto)
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Could not load payments", e);
        }
    }

    private PaymentDto toDto(DocumentSnapshot document) {
        return new PaymentDto(
                document.getId(),
                document.getString("ticketId"),
                document.getString("userId"),
                FirestoreMapper.longValue(document, "tripId"),
                FirestoreMapper.longValue(document, "seatId"),
                FirestoreMapper.doubleValue(document, "amount"),
                document.getString("provider"),
                document.getString("status"),
                document.getString("vnpTxnRef"),
                document.getString("vnpTransactionNo"),
                FirestoreMapper.longValue(document, "createdAt"),
                FirestoreMapper.longValue(document, "updatedAt")
        );
    }

}
