package com.dtthuan3.ecommerce.analyticsservice.dto.response;

/** KPI tong quan dashboard ADMIN. pendingRefundOrders la placeholder Phase refund. */
public record KpisResponse(
        long todayRevenue,
        int todayOrders,
        long last7DaysRevenue,
        long last30DaysRevenue,
        long avgOrderValue30d,
        int pendingRefundOrders) {}
