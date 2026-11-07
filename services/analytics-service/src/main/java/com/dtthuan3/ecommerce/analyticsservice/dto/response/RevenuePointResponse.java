package com.dtthuan3.ecommerce.analyticsservice.dto.response;

import java.time.LocalDate;

/** 1 diem doanh thu theo ngay (hoac dau thang khi granularity=month). */
public record RevenuePointResponse(
        LocalDate statDate,
        long grossRevenue,
        long netRevenue,
        int orderCount) {}
