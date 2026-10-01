package com.dtthuan3.ecommerce.orderservice;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrderServiceApplication {
    public static void main(String[] args) {
        // Postgres tren VPS khong co tzdata Asia/Saigon; ep UTC de driver
        // khong gui TimeZone la gay FATAL khi handshake.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
