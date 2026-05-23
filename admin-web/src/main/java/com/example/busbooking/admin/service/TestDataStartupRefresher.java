package com.example.busbooking.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TestDataStartupRefresher {
    private static final Logger log = LoggerFactory.getLogger(TestDataStartupRefresher.class);

    private final TestDataMaintenanceService testDataMaintenanceService;

    public TestDataStartupRefresher(TestDataMaintenanceService testDataMaintenanceService) {
        this.testDataMaintenanceService = testDataMaintenanceService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        try {
            log.info("Refreshing rolling test data on admin-web startup");
            testDataMaintenanceService.refreshRollingTrips();
        } catch (Exception e) {
            log.warn("Could not refresh rolling test data on startup", e);
        }
    }
}
