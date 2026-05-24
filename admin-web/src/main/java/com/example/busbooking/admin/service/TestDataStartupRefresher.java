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
    private final VnpayPaymentService vnpayPaymentService;

    public TestDataStartupRefresher(
            TestDataMaintenanceService testDataMaintenanceService,
            VnpayPaymentService vnpayPaymentService
    ) {
        this.testDataMaintenanceService = testDataMaintenanceService;
        this.vnpayPaymentService = vnpayPaymentService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        try {
            log.info("Refreshing rolling test data on admin-web startup");
            testDataMaintenanceService.refreshRollingTrips();
        } catch (Exception e) {
            log.warn("Could not refresh rolling test data on startup", e);
        }
        try {
            int reconciled = vnpayPaymentService.reconcileSuccessfulPayments();
            if (reconciled > 0) {
                log.info("Reconciled {} successful VNPAY payment(s)", reconciled);
            }
        } catch (Exception e) {
            log.warn("Could not reconcile successful VNPAY payments on startup", e);
        }
    }
}
