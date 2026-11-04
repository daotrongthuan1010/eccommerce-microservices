package com.dtthuan3.ecommerce.paymentservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job nen: (1) expire payment PENDING qua han 15 phut; (2) doi soat payment PENDING
 * lau chua co IPN bang query sang gateway (cuu case mat IPN).
 */
@Component
public class PaymentMaintenanceJobs {

    private static final Logger log = LoggerFactory.getLogger(PaymentMaintenanceJobs.class);
    private final PaymentService paymentService;

    public PaymentMaintenanceJobs(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Scheduled(fixedDelay = 60000)
    public void expire() {
        int n = paymentService.expireOverdue();
        if (n > 0) log.info("Expired {} overdue payments", n);
    }

    @Scheduled(fixedDelay = 120000)
    public void reconcile() {
        int n = paymentService.reconcileStale();
        if (n > 0) log.info("Reconciled {} stale payments", n);
    }
}
