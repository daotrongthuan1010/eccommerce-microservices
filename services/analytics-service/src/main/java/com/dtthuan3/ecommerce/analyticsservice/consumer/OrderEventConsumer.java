package com.dtthuan3.ecommerce.analyticsservice.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhan OrderPaid tu topic order-events, cong don doanh thu theo ngay (gio VN).
 * Idempotent: dedup theo eventId qua processed_events (at-least-once -> effectively-once).
 * OrderCreated/OrderCancelled chi log debug — Phase refund xu ly sau.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${analytics.kafka.order-events-topic:order-events}")
    @Transactional
    public void onOrderEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);

            String eventId = text(event, "eventId");
            String eventType = text(event, "eventType");
            if (eventId == null || eventType == null) {
                log.warn("Bo qua order event sai shape: {}", truncate(message));
                return;
            }
            if (!"OrderPaid".equals(eventType)) {
                log.debug("Order event {} khong tinh doanh thu: bo qua", eventType);
                return;
            }

            // 1. Dedup: event xu ly roi thi bo qua
            int inserted = jdbc.update(
                    "INSERT INTO processed_events (event_id) VALUES (?) ON CONFLICT DO NOTHING",
                    eventId);
            if (inserted == 0) {
                log.debug("Order event {} da xu ly, bo qua", eventId);
                return;
            }

            // 2. Quy occurredAt ve ngay VN (mac dinh hom nay gio VN neu thieu/sai)
            LocalDate statDate = statDate(event);
            String currency = text(event, "currency");
            if (currency == null || currency.isBlank()) {
                currency = "VND";
            }
            long totalAmount = event.path("totalAmount").asLong(0);

            // 3. Cong don doanh thu ngay
            jdbc.update(
                    """
                    INSERT INTO daily_revenue
                        (stat_date, currency, gross_revenue, refunded_revenue, net_revenue, order_count)
                    VALUES (?, ?, ?, 0, ?, 1)
                    ON CONFLICT (stat_date, currency) DO UPDATE SET
                        gross_revenue = daily_revenue.gross_revenue + EXCLUDED.gross_revenue,
                        order_count = daily_revenue.order_count + 1,
                        net_revenue = daily_revenue.gross_revenue + EXCLUDED.gross_revenue
                            - daily_revenue.refunded_revenue
                    """,
                    statDate, currency, totalAmount, totalAmount);

            // 4. Cong don theo tung SKU
            JsonNode lines = event.path("lines");
            if (lines.isArray()) {
                for (JsonNode line : lines) {
                    String skuId = text(line, "skuId");
                    if (skuId == null || skuId.isBlank()) {
                        continue;
                    }
                    String productName = text(line, "productName");
                    if (productName == null || productName.isBlank()) {
                        productName = skuId; // payload cu khong co ten — dung tam skuId
                    }
                    long quantity = line.path("quantity").asLong(0);
                    long revenue = line.path("lineAmount").asLong(0);
                    jdbc.update(
                            """
                            INSERT INTO product_sales
                                (stat_date, sku_id, product_name, quantity, revenue)
                            VALUES (?, ?, ?, ?, ?)
                            ON CONFLICT (stat_date, sku_id) DO UPDATE SET
                                quantity = product_sales.quantity + EXCLUDED.quantity,
                                revenue = product_sales.revenue + EXCLUDED.revenue,
                                product_name = EXCLUDED.product_name
                            """,
                            statDate, skuId, productName, quantity, revenue);
                }
            }

            log.info("Da tong hop OrderPaid {} ngay {} tong {}", eventId, statDate, totalAmount);
        } catch (Exception e) {
            // Poison message: log + khong rethrow de khong block consumer; can DLT sau nay
            log.error("Xu ly order event that bai: {} — {}", e.getMessage(), truncate(message), e);
        }
    }

    private static LocalDate statDate(JsonNode event) {
        try {
            String occurredAt = text(event, "occurredAt");
            if (occurredAt != null) {
                return Instant.parse(occurredAt).atZone(VN).toLocalDate();
            }
        } catch (Exception e) {
            log.warn("occurredAt sai dinh dang, dung ngay hom nay (gio VN)");
        }
        return LocalDate.now(VN);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static String truncate(String s) {
        return s != null && s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }
}
