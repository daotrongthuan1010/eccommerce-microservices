package com.dtthuan3.ecommerce.analyticsservice.service;

import com.dtthuan3.ecommerce.analyticsservice.dto.response.KpisResponse;
import com.dtthuan3.ecommerce.analyticsservice.dto.response.RevenuePointResponse;
import com.dtthuan3.ecommerce.analyticsservice.dto.response.TopProductResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Truy van bao cao doanh thu tu bang tong hop (JdbcTemplate, read-only).
 * Moi so lieu tinh theo gio VN cho khop voi consumer.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<RevenuePointResponse> revenue(LocalDate from, LocalDate to, boolean byMonth) {
        String dateExpr = byMonth ? "date_trunc('month', stat_date)::date" : "stat_date";
        return jdbc.query(
                """
                SELECT %s AS bucket,
                       COALESCE(SUM(gross_revenue), 0) AS gross,
                       COALESCE(SUM(net_revenue), 0) AS net,
                       COALESCE(SUM(order_count), 0) AS orders
                FROM daily_revenue
                WHERE stat_date BETWEEN ? AND ?
                GROUP BY bucket
                ORDER BY bucket
                """.formatted(dateExpr),
                (rs, i) -> new RevenuePointResponse(
                        rs.getObject("bucket", LocalDate.class),
                        rs.getLong("gross"),
                        rs.getLong("net"),
                        rs.getInt("orders")),
                from, to);
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> topProducts(LocalDate from, LocalDate to, int limit) {
        return jdbc.query(
                """
                SELECT sku_id, MAX(product_name) AS product_name,
                       COALESCE(SUM(quantity), 0) AS qty,
                       COALESCE(SUM(revenue), 0) AS rev
                FROM product_sales
                WHERE stat_date BETWEEN ? AND ?
                GROUP BY sku_id
                ORDER BY rev DESC
                LIMIT ?
                """,
                (rs, i) -> new TopProductResponse(
                        rs.getString("sku_id"),
                        rs.getString("product_name"),
                        rs.getLong("qty"),
                        rs.getLong("rev")),
                from, to, limit);
    }

    @Transactional(readOnly = true)
    public KpisResponse kpis() {
        LocalDate today = LocalDate.now(VN);
        long[] t = dayAgg(today);
        long last7 = rangeRevenue(today.minusDays(6), today);
        long last30 = rangeRevenue(today.minusDays(29), today);
        int orders30 = rangeOrders(today.minusDays(29), today);
        long avg = orders30 == 0 ? 0 : last30 / orders30; // lam tron xuong, VND
        return new KpisResponse(t[0], (int) t[1], last7, last30, avg, 0);
    }

    /** [grossRevenue, orderCount] cua 1 ngay. */
    private long[] dayAgg(LocalDate date) {
        return jdbc.query(
                "SELECT COALESCE(SUM(gross_revenue), 0) AS gross,"
                        + " COALESCE(SUM(order_count), 0) AS orders"
                        + " FROM daily_revenue WHERE stat_date = ?",
                rs -> {
                    if (rs.next()) {
                        return new long[] {rs.getLong("gross"), rs.getLong("orders")};
                    }
                    return new long[] {0, 0};
                },
                date);
    }

    private long rangeRevenue(LocalDate from, LocalDate to) {
        Long v = jdbc.queryForObject(
                "SELECT COALESCE(SUM(gross_revenue), 0) FROM daily_revenue"
                        + " WHERE stat_date BETWEEN ? AND ?",
                Long.class, from, to);
        return v == null ? 0 : v;
    }

    private int rangeOrders(LocalDate from, LocalDate to) {
        Integer v = jdbc.queryForObject(
                "SELECT COALESCE(SUM(order_count), 0) FROM daily_revenue"
                        + " WHERE stat_date BETWEEN ? AND ?",
                Integer.class, from, to);
        return v == null ? 0 : v;
    }
}
