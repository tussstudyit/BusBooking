package com.example.busbooking.admin.service;

import com.example.busbooking.admin.model.BusDto;
import com.example.busbooking.admin.model.BusForm;
import com.example.busbooking.admin.model.SeatDto;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BusAdminService {
    private static final Logger log = LoggerFactory.getLogger(BusAdminService.class);
    private static final int DEFAULT_BUS_LIMIT = 100;

    private final Firestore firestore;
    private final FirestoreIdService idService;

    public BusAdminService(Firestore firestore, FirestoreIdService idService) {
        this.firestore = firestore;
        this.idService = idService;
    }

    public List<BusDto> findAll() {
        try {
            return firestore.collection("buses")
                    .limit(DEFAULT_BUS_LIMIT)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(this::toDto)
                    .sorted((a, b) -> a.busName().compareToIgnoreCase(b.busName()))
                    .toList();
        } catch (Exception e) {
            log.warn("Could not load buses from Firestore", e);
            throw new IllegalStateException("Could not load buses", e);
        }
    }

    public BusDto findByDocumentId(String documentId) {
        try {
            DocumentSnapshot document = firestore.collection("buses").document(documentId).get().get();
            if (!document.exists()) {
                throw new IllegalArgumentException("Bus not found");
            }
            return toDto(document);
        } catch (Exception e) {
            throw new IllegalStateException("Could not load bus", e);
        }
    }

    public void create(BusForm form) {
        long id = idService.nextNumericId("buses");
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("busName", form.getBusName().trim());
        data.put("totalSeats", form.getTotalSeats());
        data.put("licensePlate", form.getLicensePlate().trim());
        data.put("seatLayoutJson", "");
        data.put("isActive", form.getIsActive() == null || form.getIsActive());
        data.put("createdAt", System.currentTimeMillis());
        try {
            firestore.collection("buses").document(String.valueOf(id)).set(data).get();
        } catch (Exception e) {
            throw new IllegalStateException("Could not create bus", e);
        }
    }

    public void update(String documentId, BusForm form) {
        Map<String, Object> data = new HashMap<>();
        data.put("busName", form.getBusName().trim());
        data.put("totalSeats", form.getTotalSeats());
        data.put("licensePlate", form.getLicensePlate().trim());
        data.put("isActive", form.getIsActive() == null || form.getIsActive());
        try {
            firestore.collection("buses").document(documentId).update(data).get();
        } catch (Exception e) {
            throw new IllegalStateException("Could not update bus", e);
        }
    }

    public void setActive(String documentId, boolean active) {
        try {
            firestore.collection("buses").document(documentId).update("isActive", active).get();
        } catch (Exception e) {
            throw new IllegalStateException("Could not update bus status", e);
        }
    }

    public List<SeatDto> findSeats(String busDocumentId) {
        try {
            return firestore.collection("buses").document(busDocumentId).collection("seats")
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(this::toSeatDto)
                    .sorted((a, b) -> a.seatNumber().compareToIgnoreCase(b.seatNumber()))
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Could not load seats", e);
        }
    }

    public void generateSimpleSeats(String busDocumentId) {
        BusDto bus = findByDocumentId(busDocumentId);
        int totalSeats = bus.totalSeats() == null ? 0 : bus.totalSeats();
        if (totalSeats <= 0 || totalSeats > 80) {
            throw new IllegalArgumentException("Invalid total seats");
        }

        try {
            for (int i = 1; i <= totalSeats; i++) {
                int floor = i <= Math.ceil(totalSeats / 2.0) ? 1 : 2;
                Map<String, Object> data = new HashMap<>();
                data.put("id", (long) i);
                data.put("busId", bus.id());
                data.put("seatNumber", (floor == 1 ? "A" : "B") + i);
                data.put("floor", floor);
                data.put("rowIndex", (i - 1) / 4);
                data.put("columnIndex", (i - 1) % 4);
                data.put("isWindow", i % 4 == 1 || i % 4 == 0);
                data.put("isAisle", i % 4 == 2 || i % 4 == 3);
                data.put("seatType", "STANDARD");
                data.put("createdAt", System.currentTimeMillis());
                firestore.collection("buses").document(busDocumentId)
                        .collection("seats").document(String.valueOf(i)).set(data).get();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate seats", e);
        }
    }

    private BusDto toDto(DocumentSnapshot document) {
        return new BusDto(
                document.getId(),
                FirestoreMapper.longValue(document, "id"),
                document.getString("busName"),
                FirestoreMapper.intValue(document, "totalSeats"),
                document.getString("licensePlate"),
                document.getString("seatLayoutJson"),
                document.getBoolean("isActive"),
                FirestoreMapper.longValue(document, "createdAt")
        );
    }

    private SeatDto toSeatDto(DocumentSnapshot document) {
        return new SeatDto(
                document.getId(),
                FirestoreMapper.longValue(document, "id"),
                FirestoreMapper.longValue(document, "busId"),
                document.getString("seatNumber"),
                FirestoreMapper.intValue(document, "floor"),
                FirestoreMapper.intValue(document, "rowIndex"),
                FirestoreMapper.intValue(document, "columnIndex"),
                document.getBoolean("isWindow"),
                document.getBoolean("isAisle"),
                document.getString("seatType"),
                FirestoreMapper.longValue(document, "createdAt")
        );
    }
}
