package com.example.busbooking.admin.service;

import com.example.busbooking.admin.model.TicketDto;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TicketAdminService {
    private static final int DEFAULT_TICKET_LIMIT = 200;

    private final Firestore firestore;

    public TicketAdminService(Firestore firestore) {
        this.firestore = firestore;
    }

    public List<TicketDto> findAll() {
        try {
            return firestore.collection("tickets")
                    .orderBy("bookingTime", Query.Direction.DESCENDING)
                    .limit(DEFAULT_TICKET_LIMIT)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(this::toDto)
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Could not load tickets", e);
        }
    }

    private TicketDto toDto(DocumentSnapshot document) {
        return new TicketDto(
                document.getId(),
                FirestoreMapper.longValue(document, "id"),
                stringValue(document, "userId"),
                FirestoreMapper.longValue(document, "tripId"),
                FirestoreMapper.longValue(document, "seatId"),
                FirestoreMapper.longValue(document, "busId"),
                document.getString("paymentId"),
                FirestoreMapper.longValue(document, "bookingTime"),
                document.getString("status"),
                document.getString("refundStatus")
        );
    }

    private String stringValue(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        return value == null ? null : String.valueOf(value);
    }

}
