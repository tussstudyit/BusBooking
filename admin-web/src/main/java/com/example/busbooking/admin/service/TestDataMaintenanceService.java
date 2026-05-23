package com.example.busbooking.admin.service;

import com.example.busbooking.admin.model.RouteCatalogEntry;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class TestDataMaintenanceService {
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_KEY = DateTimeFormatter.BASIC_ISO_DATE;
    private static final long HOUR_MS = 3_600_000L;
    private static final long MINUTE_MS = 60_000L;
    private static final int WINDOW_DAYS = 3;
    private static final int TICKET_RETENTION_DAYS = 3;
    private static final String GENERATED_BY = "rolling-test-data";

    private final Firestore firestore;
    private final RouteCatalogService routeCatalogService;

    private record TimeSlot(int hour, int minute) {
        String key() {
            return "%02d%02d".formatted(hour, minute);
        }
    }

    private static final class CleanupStats {
        int deletedTickets;
        int deletedPayments;
        int deletedTripSeats;
        int deletedTrips;
    }

    public TestDataMaintenanceService(Firestore firestore, RouteCatalogService routeCatalogService) {
        this.firestore = firestore;
        this.routeCatalogService = routeCatalogService;
    }

    public Map<String, Object> refreshRollingTrips() {
        try {
            LocalDate today = LocalDate.now(VN_ZONE);
            long retentionStart = dayStart(today.minusDays(TICKET_RETENTION_DAYS));

            List<DocumentSnapshot> routeDocuments = ensureRoutes();
            List<DocumentSnapshot> busDocuments = ensureBuses();
            LocalDate firstTripDate = hasFutureSlotToday(today, routeDocuments) ? today : today.plusDays(1);
            long windowStart = dayStart(firstTripDate);
            long windowEnd = dayStart(firstTripDate.plusDays(WINDOW_DAYS));
            int generatedSeats = ensureSeatLayouts(busDocuments);
            CleanupStats cleanup = cleanupOldData(retentionStart, windowEnd);
            int createdTrips = ensureTrips(today, firstTripDate, routeDocuments, busDocuments);

            Map<String, Object> result = new HashMap<>();
            result.put("code", "00");
            result.put("message", "Rolling test data refreshed");
            result.put("windowDays", WINDOW_DAYS);
            result.put("ticketRetentionDays", TICKET_RETENTION_DAYS);
            result.put("windowStart", windowStart);
            result.put("windowEnd", windowEnd);
            result.put("routes", routeDocuments.size());
            result.put("buses", busDocuments.size());
            result.put("createdTrips", createdTrips);
            result.put("generatedSeats", generatedSeats);
            result.put("deletedTickets", cleanup.deletedTickets);
            result.put("deletedPayments", cleanup.deletedPayments);
            result.put("deletedTripSeats", cleanup.deletedTripSeats);
            result.put("deletedTrips", cleanup.deletedTrips);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Could not refresh rolling test data", e);
        }
    }

    private List<DocumentSnapshot> ensureRoutes() throws Exception {
        List<DocumentSnapshot> activeRoutes = loadActiveRoutes();
        if (!activeRoutes.isEmpty()) {
            return activeRoutes;
        }

        long now = System.currentTimeMillis();
        long id = 1L;
        for (RouteCatalogEntry route : routeCatalogService.routes()) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", id);
            data.put("originId", route.originId());
            data.put("destinationId", route.destinationId());
            data.put("origin", route.originName());
            data.put("destination", route.destinationName());
            data.put("distance", route.distanceKm());
            data.put("suggestedPrice", route.price());
            data.put("durationMs", route.durationMs());
            data.put("isActive", true);
            data.put("createdAt", now);
            firestore.collection("routes").document(String.valueOf(id)).set(data).get();
            id++;
        }
        return loadActiveRoutes();
    }

    private List<DocumentSnapshot> loadActiveRoutes() throws Exception {
        return firestore.collection("routes")
                .whereEqualTo("isActive", true)
                .get()
                .get()
                .getDocuments()
                .stream()
                .filter(document -> FirestoreMapper.longValue(document, "id") != null)
                .map(document -> (DocumentSnapshot) document)
                .toList();
    }

    private List<DocumentSnapshot> ensureBuses() throws Exception {
        List<DocumentSnapshot> activeBuses = loadActiveBuses();
        if (!activeBuses.isEmpty()) {
            return activeBuses;
        }

        long now = System.currentTimeMillis();
        for (int id = 1; id <= 3; id++) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", (long) id);
            data.put("busName", "Xe Tan Quang Dung");
            data.put("totalSeats", 34);
            data.put("licensePlate", "43A-%05d".formatted(12000 + id));
            data.put("seatLayoutJson", "");
            data.put("isActive", true);
            data.put("generatedBy", GENERATED_BY);
            data.put("createdAt", now);
            firestore.collection("buses").document(String.valueOf(id)).set(data).get();
        }
        return loadActiveBuses();
    }

    private List<DocumentSnapshot> loadActiveBuses() throws Exception {
        return firestore.collection("buses")
                .whereEqualTo("isActive", true)
                .get()
                .get()
                .getDocuments()
                .stream()
                .filter(document -> FirestoreMapper.longValue(document, "id") != null)
                .map(document -> (DocumentSnapshot) document)
                .toList();
    }

    private int ensureSeatLayouts(List<DocumentSnapshot> busDocuments) throws Exception {
        int created = 0;
        for (DocumentSnapshot bus : busDocuments) {
            int existingSeats = firestore.collection("buses")
                    .document(bus.getId())
                    .collection("seats")
                    .get()
                    .get()
                    .size();
            if (existingSeats > 0) {
                continue;
            }

            Long busId = FirestoreMapper.longValue(bus, "id");
            int totalSeats = FirestoreMapper.intValue(bus, "totalSeats") == null
                    ? 34
                    : FirestoreMapper.intValue(bus, "totalSeats");
            int safeTotal = Math.max(1, Math.min(totalSeats, 80));
            for (int index = 1; index <= safeTotal; index++) {
                int floor = index <= 17 ? 1 : 2;
                int floorIndex = index <= 17 ? index : index - 17;
                Map<String, Object> data = new HashMap<>();
                data.put("id", (long) index);
                data.put("busId", busId);
                data.put("seatNumber", (floor == 1 ? "A" : "B") + floorIndex);
                data.put("floor", floor);
                data.put("rowIndex", (floorIndex - 1) / 3);
                data.put("columnIndex", (floorIndex - 1) % 3);
                data.put("isWindow", floorIndex % 3 != 2);
                data.put("isAisle", floorIndex % 3 == 2);
                data.put("seatType", "STANDARD");
                data.put("generatedBy", GENERATED_BY);
                data.put("createdAt", System.currentTimeMillis());
                firestore.collection("buses")
                        .document(bus.getId())
                        .collection("seats")
                        .document(String.valueOf(index))
                        .set(data)
                        .get();
                created++;
            }
        }
        return created;
    }

    private int ensureTrips(
            LocalDate today,
            LocalDate firstTripDate,
            List<DocumentSnapshot> routeDocuments,
            List<DocumentSnapshot> busDocuments
    ) throws Exception {
        if (routeDocuments.isEmpty() || busDocuments.isEmpty()) {
            return 0;
        }

        long now = System.currentTimeMillis();
        int created = 0;
        int busIndex = 0;
        for (int dayOffset = 0; dayOffset < WINDOW_DAYS; dayOffset++) {
            LocalDate date = firstTripDate.plusDays(dayOffset);
            long tripDate = dayStart(date);
            String dateKey = date.format(DATE_KEY);

            for (DocumentSnapshot route : routeDocuments) {
                Long routeId = FirestoreMapper.longValue(route, "id");
                if (routeId == null) {
                    continue;
                }
                long durationMs = routeDurationMs(route);
                double price = routePrice(route);
                List<TimeSlot> slots = slotsFor(durationMs);

                for (TimeSlot slot : slots) {
                    long departureTime = tripDate + slot.hour() * HOUR_MS + slot.minute() * MINUTE_MS;
                    if (date.equals(today) && departureTime <= now + 10 * MINUTE_MS) {
                        continue;
                    }

                    DocumentSnapshot bus = busDocuments.get(busIndex % busDocuments.size());
                    busIndex++;
                    Long busId = FirestoreMapper.longValue(bus, "id");
                    if (busId == null) {
                        continue;
                    }

                    String documentId = "rolling_%d_%s_%s".formatted(routeId, dateKey, slot.key());
                    if (firestore.collection("trips").document(documentId).get().get().exists()) {
                        continue;
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("id", toStableLongId(documentId));
                    data.put("routeId", routeId);
                    data.put("busId", busId);
                    data.put("departureTime", departureTime);
                    data.put("arrivalTime", departureTime + durationMs);
                    data.put("price", price);
                    data.put("tripDate", tripDate);
                    data.put("status", "SCHEDULED");
                    data.put("generatedBy", GENERATED_BY);
                    data.put("createdAt", now);
                    data.put("updatedAt", now);
                    firestore.collection("trips").document(documentId).set(data).get();
                    created++;
                }
            }
        }
        return created;
    }

    private boolean hasFutureSlotToday(LocalDate today, List<DocumentSnapshot> routeDocuments) {
        long todayStart = dayStart(today);
        long now = System.currentTimeMillis();
        for (DocumentSnapshot route : routeDocuments) {
            long durationMs = routeDurationMs(route);
            for (TimeSlot slot : slotsFor(durationMs)) {
                long departureTime = todayStart + slot.hour() * HOUR_MS + slot.minute() * MINUTE_MS;
                if (departureTime > now + 10 * MINUTE_MS) {
                    return true;
                }
            }
        }
        return false;
    }

    private CleanupStats cleanupOldData(long retentionStart, long windowEnd) throws Exception {
        CleanupStats stats = new CleanupStats();
        Set<String> deletedTicketIds = new HashSet<>();
        Set<String> deletedPaymentIds = new HashSet<>();

        for (DocumentSnapshot ticket : firestore.collection("tickets")
                .whereLessThan("createdAt", retentionStart)
                .get()
                .get()
                .getDocuments()) {
            deleteTicket(ticket, deletedTicketIds, deletedPaymentIds, stats);
        }

        for (DocumentSnapshot ticket : firestore.collection("tickets")
                .whereLessThan("bookingTime", retentionStart)
                .get()
                .get()
                .getDocuments()) {
            deleteTicket(ticket, deletedTicketIds, deletedPaymentIds, stats);
        }

        for (DocumentSnapshot payment : firestore.collection("payments")
                .whereLessThan("createdAt", retentionStart)
                .get()
                .get()
                .getDocuments()) {
            deletePayment(payment.getId(), deletedPaymentIds, stats);
        }

        for (DocumentSnapshot tripSeat : firestore.collection("tripSeats")
                .whereLessThan("createdAt", retentionStart)
                .get()
                .get()
                .getDocuments()) {
            tripSeat.getReference().delete().get();
            stats.deletedTripSeats++;
        }

        List<DocumentSnapshot> generatedTrips = firestore.collection("trips")
                .whereEqualTo("generatedBy", GENERATED_BY)
                .get()
                .get()
                .getDocuments()
                .stream()
                .map(document -> (DocumentSnapshot) document)
                .toList();
        for (DocumentSnapshot trip : generatedTrips) {
            Long tripDate = FirestoreMapper.longValue(trip, "tripDate");
            if (tripDate != null && (tripDate < retentionStart || tripDate >= windowEnd)) {
                deleteTripCascade(trip, deletedTicketIds, deletedPaymentIds, stats);
            }
        }

        return stats;
    }

    private void deleteTripCascade(
            DocumentSnapshot trip,
            Set<String> deletedTicketIds,
            Set<String> deletedPaymentIds,
            CleanupStats stats
    ) throws Exception {
        Long tripId = FirestoreMapper.longValue(trip, "id");
        if (tripId != null) {
            for (DocumentSnapshot ticket : firestore.collection("tickets")
                    .whereEqualTo("tripId", tripId)
                    .get()
                    .get()
                    .getDocuments()) {
                deleteTicket(ticket, deletedTicketIds, deletedPaymentIds, stats);
            }
            for (DocumentSnapshot payment : firestore.collection("payments")
                    .whereEqualTo("tripId", tripId)
                    .get()
                    .get()
                    .getDocuments()) {
                deletePayment(payment.getId(), deletedPaymentIds, stats);
            }
            for (DocumentSnapshot tripSeat : firestore.collection("tripSeats")
                    .whereEqualTo("tripId", tripId)
                    .get()
                    .get()
                    .getDocuments()) {
                tripSeat.getReference().delete().get();
                stats.deletedTripSeats++;
            }
        }
        trip.getReference().delete().get();
        stats.deletedTrips++;
    }

    private void deleteTicket(
            DocumentSnapshot ticket,
            Set<String> deletedTicketIds,
            Set<String> deletedPaymentIds,
            CleanupStats stats
    ) throws Exception {
        if (!deletedTicketIds.add(ticket.getId())) {
            return;
        }
        Long tripId = FirestoreMapper.longValue(ticket, "tripId");
        Long seatId = FirestoreMapper.longValue(ticket, "seatId");
        if (tripId != null && seatId != null) {
            firestore.collection("tripSeats").document(tripId + "_" + seatId).delete().get();
            stats.deletedTripSeats++;
        }
        String paymentId = ticket.getString("paymentId");
        if (paymentId != null && !paymentId.isBlank()) {
            deletePayment(paymentId, deletedPaymentIds, stats);
        }
        ticket.getReference().delete().get();
        stats.deletedTickets++;
    }

    private void deletePayment(
            String paymentId,
            Set<String> deletedPaymentIds,
            CleanupStats stats
    ) throws Exception {
        if (paymentId == null || paymentId.isBlank() || !deletedPaymentIds.add(paymentId)) {
            return;
        }
        firestore.collection("payments").document(paymentId).delete().get();
        stats.deletedPayments++;
    }

    private List<TimeSlot> slotsFor(long durationMs) {
        if (durationMs < 3 * HOUR_MS) {
            return List.of(new TimeSlot(7, 0), new TimeSlot(12, 0), new TimeSlot(18, 30));
        }
        if (durationMs < 7 * HOUR_MS) {
            return List.of(new TimeSlot(6, 30), new TimeSlot(13, 0), new TimeSlot(20, 0));
        }
        return List.of(new TimeSlot(7, 0), new TimeSlot(15, 0), new TimeSlot(22, 0));
    }

    private long routeDurationMs(DocumentSnapshot route) {
        Long duration = FirestoreMapper.longValue(route, "durationMs");
        if (duration != null && duration > 0L) {
            return duration;
        }
        String originId = routeId(route, "originId", "origin");
        String destinationId = routeId(route, "destinationId", "destination");
        if (originId != null && destinationId != null) {
            return routeCatalogService.findRoute(originId, destinationId)
                    .map(RouteCatalogEntry::durationMs)
                    .orElse(4 * HOUR_MS);
        }
        return 4 * HOUR_MS;
    }

    private double routePrice(DocumentSnapshot route) {
        Long suggestedPrice = FirestoreMapper.longValue(route, "suggestedPrice");
        if (suggestedPrice != null && suggestedPrice > 0L) {
            return suggestedPrice.doubleValue();
        }
        String originId = routeId(route, "originId", "origin");
        String destinationId = routeId(route, "destinationId", "destination");
        if (originId != null && destinationId != null) {
            return routeCatalogService.price(originId, destinationId);
        }
        return 200_000.0;
    }

    private String routeId(DocumentSnapshot route, String idField, String nameField) {
        String direct = route.getString(idField);
        if (direct != null && !direct.isBlank()) {
            return direct;
        }
        return routeCatalogService.provinceIdByName(route.getString(nameField)).orElse(null);
    }

    private long dayStart(LocalDate date) {
        return date.atStartOfDay(VN_ZONE).toInstant().toEpochMilli();
    }

    private static long toStableLongId(String value) {
        long hash = 1125899906842597L;
        for (int i = 0; i < value.length(); i++) {
            hash = 31 * hash + value.charAt(i);
        }
        return hash == Long.MIN_VALUE ? 0L : Math.abs(hash);
    }
}
