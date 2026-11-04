package com.dtthuan3.ecommerce.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Snapshot dong don: gia/ten san pham ghi lai thoi diat dat (khong phu thuoc product sau nay). */
@Getter
@Setter
@Entity
@Table(name = "order_lines")
public class OrderLine {
    @Id @Column(name = "id", length = 36) private String id;
    @Column(name = "order_id", nullable = false, length = 36) private String orderId;
    @Column(name = "line_no", nullable = false) private int lineNo;
    @Column(name = "sku_id", nullable = false, length = 64) private String skuId;
    @Column(name = "product_name", nullable = false, length = 255) private String productName;
    @Column(name = "unit_price", nullable = false) private long unitPrice;
    @Column(nullable = false) private int quantity;
    @Column(name = "line_amount", nullable = false) private long lineAmount;
}
