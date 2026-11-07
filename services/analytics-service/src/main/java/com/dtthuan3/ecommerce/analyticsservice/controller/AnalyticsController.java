package com.dtthuan3.ecommerce.analyticsservice.controller;

import com.dtthuan3.ecommerce.analyticsservice.dto.response.KpisResponse;
import com.dtthuan3.ecommerce.analyticsservice.dto.response.RevenuePointResponse;
import com.dtthuan3.ecommerce.analyticsservice.dto.response.TopProductResponse;
import com.dtthuan3.ecommerce.analyticsservice.service.AnalyticsService;
import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.response.ApiResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bao cao doanh thu cho ADMIN. Convention gateway StripPrefix=2: /api/analytics/kpis
 * -> /kpis (controller KHONG lap ten service — nhu review /admin/reviews, order /me).
 * from/to dang yyyy-MM-dd, mac dinh 30 ngay gan nhat (gio VN).
 */
@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class AnalyticsController {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final AnalyticsService analyticsService;

    @GetMapping("/revenue")
    public ApiResponse<List<RevenuePointResponse>> revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String granularity) {
        LocalDate[] range = resolveRange(from, to);
        boolean byMonth = switch (granularity.toLowerCase()) {
            case "month" -> true;
            case "day" -> false;
            default -> throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "granularity chi nhan day|month");
        };
        return ApiResponse.ok(analyticsService.revenue(range[0], range[1], byMonth));
    }

    @GetMapping("/top-products")
    public ApiResponse<List<TopProductResponse>> topProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        LocalDate[] range = resolveRange(from, to);
        if (limit < 1 || limit > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "limit phai trong 1..100");
        }
        return ApiResponse.ok(analyticsService.topProducts(range[0], range[1], limit));
    }

    @GetMapping("/kpis")
    public ApiResponse<KpisResponse> kpis() {
        return ApiResponse.ok(analyticsService.kpis());
    }

    /** Mac dinh 30 ngay gan nhat; from > to thi 400. */
    private static LocalDate[] resolveRange(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now(VN);
        LocalDate start = from != null ? from : end.minusDays(29);
        if (start.isAfter(end)) {
            throw new BusinessException(ErrorCode.INVALID_TIME_RANGE, "from phai <= to");
        }
        return new LocalDate[] {start, end};
    }
}
