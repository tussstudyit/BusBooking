package com.example.busbooking.admin.controller;

import com.example.busbooking.admin.service.TestDataMaintenanceService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestDataController {
    private final TestDataMaintenanceService testDataMaintenanceService;

    public TestDataController(TestDataMaintenanceService testDataMaintenanceService) {
        this.testDataMaintenanceService = testDataMaintenanceService;
    }

    @PostMapping("/api/test-data/refresh")
    public Map<String, Object> refresh() {
        try {
            return testDataMaintenanceService.refreshRollingTrips();
        } catch (IllegalStateException e) {
            return Map.of(
                    "code", "99",
                    "message", rootMessage(e)
            );
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? throwable.getMessage() : current.getMessage();
    }
}
