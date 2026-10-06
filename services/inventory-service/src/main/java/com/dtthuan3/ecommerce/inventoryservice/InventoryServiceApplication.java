package com.dtthuan3.ecommerce.inventoryservice;

import java.util.TimeZone;
import com.dtthuan3.ecommerce.inventoryservice.config.InventoryBatchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableConfigurationProperties(InventoryBatchProperties.class)
// Tự gán createdAt/updatedAt khi các entity được lưu hoặc cập nhật qua JPA.
@EnableJpaAuditing
public class InventoryServiceApplication {
    public static void main(String[] args) {
        // PostgreSQL trên VPS không có tzdata Asia/Saigon; đặt UTC để JDBC
        // không gửi múi giờ không được hỗ trợ khi bắt tay kết nối.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
