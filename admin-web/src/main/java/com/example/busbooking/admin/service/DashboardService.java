package com.example.busbooking.admin.service;

import com.example.busbooking.admin.model.DashboardStats;
import com.google.cloud.firestore.Firestore;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private static final long DAY_MS = 86_400_000L;
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final Firestore firestore;

    public DashboardService(Firestore firestore) {
        this.firestore = firestore;
    }

    public DashboardStats getStats() {
        return new DashboardStats(
                count("users"),
                countWhere("routes", "isActive", true),
                countWhere("buses", "isActive", true),
                countTodayTrips(),
                count("tickets"),
                countWhere("payments", "status", "SUCCESS")
        );
    }

    private long count(String collection) {
        try {
            return firestore.collection(collection).count().get().get().getCount();
        } catch (Exception e) {
            return 0L;
        }
    }

    private long countWhere(String collection, String field, Object value) {
        try {
            return firestore.collection(collection).whereEqualTo(field, value).count().get().get().getCount();
        } catch (Exception e) {
            return 0L;
        }
    }

    private long countTodayTrips() {
        try {
            long todayStart = LocalDate.now(VN_ZONE).atStartOfDay(VN_ZONE).toInstant().toEpochMilli();
            return firestore.collection("trips")
                    .whereGreaterThanOrEqualTo("tripDate", todayStart)
                    .whereLessThan("tripDate", todayStart + DAY_MS)
                    .count()
                    .get()
                    .get()
                    .getCount();
        } catch (Exception e) {
            return 0L;
        }
    }
}
