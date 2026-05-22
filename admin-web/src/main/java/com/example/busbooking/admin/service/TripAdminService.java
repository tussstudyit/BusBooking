package com.example.busbooking.admin.service;

import com.example.busbooking.admin.model.TripDto;
import com.example.busbooking.admin.model.TripForm;
import com.example.busbooking.admin.model.TripSeatView;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TripAdminService {
    private static final Logger log = LoggerFactory.getLogger(TripAdminService.class);
    private static final int DEFAULT_TRIP_LIMIT = 80;
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final Firestore firestore;
    private final FirestoreIdService idService;

    private record SeatAvailability(int totalSeats, int bookedSeats) {
        int availableSeats() {
            return Math.max(totalSeats - bookedSeats, 0);
        }
    }

    public TripAdminService(Firestore firestore, FirestoreIdService idService) {
        this.firestore = firestore;
        this.idService = idService;
    }

    public List<TripDto> findAll() {
        try {
            Map<Long, String> routeLabels = loadRouteLabels();
            Map<Long, String> busLicensePlates = loadBusLicensePlates();
            long todayStart = LocalDate.now(VN_ZONE).atStartOfDay(VN_ZONE).toInstant().toEpochMilli();
            return firestore.collection("trips")
                    .whereGreaterThanOrEqualTo("tripDate", todayStart)
                    .orderBy("tripDate", Query.Direction.ASCENDING)
                    .limit(DEFAULT_TRIP_LIMIT)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(document -> toDto(document, routeLabels, busLicensePlates))
                    .sorted(Comparator
                            .comparingLong((TripDto trip) -> nullToZero(trip.tripDate()))
                            .thenComparingLong(trip -> nullToZero(trip.departureTime())))
                    .toList();
        } catch (Exception e) {
            log.warn("Could not load trips from Firestore", e);
            throw new IllegalStateException("Could not load trips", e);
        }
    }

    public TripDto findByDocumentId(String documentId) {
        try {
            DocumentSnapshot document = firestore.collection("trips").document(documentId).get().get();
            if (!document.exists()) {
                throw new IllegalArgumentException("Trip not found");
            }
            return toDto(document);
        } catch (Exception e) {
            throw new IllegalStateException("Could not load trip", e);
        }
    }

    public void create(TripForm form) {
        long id = idService.nextNumericId("trips");
        Map<String, Object> data = toMap(form);
        data.put("id", id);
        data.put("createdAt", System.currentTimeMillis());
        try {
            firestore.collection("trips").document(String.valueOf(id)).set(data).get();
        } catch (Exception e) {
            throw new IllegalStateException("Could not create trip", e);
        }
    }

    public void update(String documentId, TripForm form) {
        try {
            firestore.collection("trips").document(documentId).update(toMap(form)).get();
        } catch (Exception e) {
            throw new IllegalStateException("Could not update trip", e);
        }
    }

    public void cancel(String documentId) {
        try {
            firestore.collection("trips").document(documentId).update("status", "CANCELLED").get();
        } catch (Exception e) {
            throw new IllegalStateException("Could not cancel trip", e);
        }
    }

    public List<TripSeatView> findSeatViews(String tripDocumentId) {
        TripDto trip = findByDocumentId(tripDocumentId);
        if (trip.busId() == null) {
            return List.of();
        }

        try {
            String busDocumentId = findBusDocumentId(trip.busId());
            if (busDocumentId == null) {
                return List.of();
            }

            Map<Long, String> statusBySeatId = firestore.collection("tripSeats")
                    .whereEqualTo("tripId", trip.id())
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .filter(document -> FirestoreMapper.longValue(document, "seatId") != null)
                    .collect(Collectors.toMap(
                            document -> FirestoreMapper.longValue(document, "seatId"),
                            document -> seatStatus(document.getString("status")),
                            (first, second) -> first
                    ));

            List<TripSeatView> seatViews = firestore.collection("buses")
                    .document(busDocumentId)
                    .collection("seats")
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(document -> toSeatView(document, statusBySeatId))
                    .sorted(Comparator
                            .comparing((TripSeatView seat) -> seat.floor() == null ? 0 : seat.floor())
                            .thenComparing(seat -> seat.seatId() == null ? 0L : seat.seatId()))
                    .toList();
            if (!seatViews.isEmpty()) {
                return seatViews;
            }
            return defaultSeatViews(statusBySeatId);
        } catch (Exception e) {
            throw new IllegalStateException("Could not load trip seats", e);
        }
    }

    private Map<String, Object> toMap(TripForm form) {
        Map<String, Object> data = new HashMap<>();
        data.put("routeId", form.getRouteId());
        data.put("busId", form.getBusId());
        data.put("departureTime", form.getDepartureTime());
        data.put("arrivalTime", form.getArrivalTime());
        data.put("price", form.getPrice());
        data.put("tripDate", form.getTripDate());
        data.put("status", form.getStatus() == null || form.getStatus().isBlank() ? "SCHEDULED" : form.getStatus());
        return data;
    }

    private TripDto toDto(DocumentSnapshot document) {
        Long routeId = FirestoreMapper.longValue(document, "routeId");
        Long busId = FirestoreMapper.longValue(document, "busId");
        return toDto(
                document,
                Map.of(routeId == null ? -1L : routeId, routeLabel(routeId)),
                Map.of(busId == null ? -1L : busId, busLicensePlate(busId))
        );
    }

    private TripDto toDto(DocumentSnapshot document, Map<Long, String> routeLabels, Map<Long, String> busLicensePlates) {
        Long routeId = FirestoreMapper.longValue(document, "routeId");
        Long busId = FirestoreMapper.longValue(document, "busId");
        Long tripId = FirestoreMapper.longValue(document, "id");
        String status = document.getString("status");
        SeatAvailability availability = seatAvailability(tripId, busId);
        return new TripDto(
                document.getId(),
                tripId,
                routeId,
                busId,
                routeLabels.getOrDefault(routeId, routeId == null ? "" : String.valueOf(routeId)),
                busLicensePlates.getOrDefault(busId, busId == null ? "" : String.valueOf(busId)),
                FirestoreMapper.longValue(document, "departureTime"),
                FirestoreMapper.longValue(document, "arrivalTime"),
                FirestoreMapper.doubleValue(document, "price"),
                FirestoreMapper.longValue(document, "tripDate"),
                status,
                bookingStatus(status, availability),
                availability.availableSeats(),
                availability.totalSeats(),
                FirestoreMapper.longValue(document, "createdAt")
        );
    }

    private Map<Long, String> loadRouteLabels() throws Exception {
        Map<Long, String> labels = new HashMap<>();
        for (DocumentSnapshot route : firestore.collection("routes").get().get().getDocuments()) {
            Long id = FirestoreMapper.longValue(route, "id");
            if (id != null) {
                labels.put(id, route.getString("origin") + " -> " + route.getString("destination"));
            }
        }
        return labels;
    }

    private Map<Long, String> loadBusLicensePlates() throws Exception {
        Map<Long, String> plates = new HashMap<>();
        for (DocumentSnapshot bus : firestore.collection("buses").get().get().getDocuments()) {
            Long id = FirestoreMapper.longValue(bus, "id");
            String plate = bus.getString("licensePlate");
            if (id != null && plate != null) {
                plates.put(id, plate);
            }
        }
        return plates;
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private TripSeatView toSeatView(DocumentSnapshot document, Map<Long, String> statusBySeatId) {
        Long seatId = FirestoreMapper.longValue(document, "id");
        return new TripSeatView(
                seatId,
                document.getString("seatNumber"),
                FirestoreMapper.intValue(document, "floor"),
                FirestoreMapper.intValue(document, "rowIndex"),
                FirestoreMapper.intValue(document, "columnIndex"),
                statusBySeatId.getOrDefault(seatId, "AVAILABLE")
        );
    }

    private String seatStatus(String status) {
        if ("CONFIRMED".equals(status) || "USED".equals(status)) {
            return "BOOKED";
        }
        return "AVAILABLE";
    }

    private List<TripSeatView> defaultSeatViews(Map<Long, String> statusBySeatId) {
        return java.util.stream.IntStream.rangeClosed(1, 34)
                .mapToObj(index -> {
                    long seatId = index;
                    return new TripSeatView(
                            seatId,
                            seatNumber(index),
                            index <= 17 ? 1 : 2,
                            ((index - 1) % 17) / 3,
                            (index - 1) % 3,
                            statusBySeatId.getOrDefault(seatId, "AVAILABLE")
                    );
                })
                .toList();
    }

    private String seatNumber(int index) {
        String prefix = index <= 17 ? "A" : "B";
        int number = index <= 17 ? index : index - 17;
        return prefix + number;
    }

    private SeatAvailability seatAvailability(Long tripId, Long busId) {
        if (tripId == null || busId == null) {
            return new SeatAvailability(0, 0);
        }
        try {
            String busDocumentId = findBusDocumentId(busId);
            if (busDocumentId == null) {
                return new SeatAvailability(0, 0);
            }

            DocumentSnapshot bus = firestore.collection("buses").document(busDocumentId).get().get();
            Integer configuredTotal = FirestoreMapper.intValue(bus, "totalSeats");
            int generatedTotal = firestore.collection("buses")
                    .document(busDocumentId)
                    .collection("seats")
                    .get()
                    .get()
                    .size();
            int total = generatedTotal > 0 ? generatedTotal : (configuredTotal == null ? 0 : configuredTotal);
            int booked = firestore.collection("tripSeats")
                    .whereEqualTo("tripId", tripId)
                    .whereIn("status", List.of("CONFIRMED", "USED"))
                    .get()
                    .get()
                    .size();
            return new SeatAvailability(total, booked);
        } catch (Exception e) {
            log.warn("Could not load seat availability for trip {}", tripId, e);
            return new SeatAvailability(0, 0);
        }
    }

    private String bookingStatus(String status, SeatAvailability availability) {
        if (!"SCHEDULED".equals(status)) {
            return status == null || status.isBlank() ? "UNKNOWN" : status;
        }
        if (availability.totalSeats() <= 0) {
            return "NO_SEAT_LAYOUT";
        }
        if (availability.availableSeats() <= 0) {
            return "FULL";
        }
        return "SCHEDULED";
    }

    private String routeLabel(Long routeId) {
        if (routeId == null) {
            return "";
        }
        try {
            DocumentSnapshot route = firestore.collection("routes").document(String.valueOf(routeId)).get().get();
            if (!route.exists()) {
                return String.valueOf(routeId);
            }
            return route.getString("origin") + " -> " + route.getString("destination");
        } catch (Exception e) {
            return String.valueOf(routeId);
        }
    }

    private String busLicensePlate(Long busId) {
        if (busId == null) {
            return "";
        }
        try {
            String documentId = findBusDocumentId(busId);
            if (documentId == null) {
                return String.valueOf(busId);
            }
            DocumentSnapshot bus = firestore.collection("buses").document(documentId).get().get();
            return bus.getString("licensePlate") == null ? String.valueOf(busId) : bus.getString("licensePlate");
        } catch (Exception e) {
            return String.valueOf(busId);
        }
    }

    private String findBusDocumentId(Long busId) throws Exception {
        DocumentSnapshot direct = firestore.collection("buses").document(String.valueOf(busId)).get().get();
        if (direct.exists()) {
            return direct.getId();
        }
        return firestore.collection("buses")
                .whereEqualTo("id", busId)
                .limit(1)
                .get()
                .get()
                .getDocuments()
                .stream()
                .findFirst()
                .map(DocumentSnapshot::getId)
                .orElse(null);
    }
}
