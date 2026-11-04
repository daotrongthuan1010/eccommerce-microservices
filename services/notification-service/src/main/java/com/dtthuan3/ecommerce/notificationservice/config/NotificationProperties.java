package com.dtthuan3.ecommerce.notificationservice.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cau hinh notification-service: provider gia lap (Mailpit SMTP/HTTP), retry policy NOTI-06,
 * worker batch. Moi truong dev/UAT: provider la Mailpit chay trong k8s, khong gui ra internet.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {

    private final Mail mail = new Mail();
    private final Retry retry = new Retry();
    private final Worker worker = new Worker();

    /** Mail provider gia lap: Mailpit API (gui qua SMTP, tra ket qua qua HTTP). */
    @Getter @Setter
    public static class Mail {
        /** Mailpit HTTP API base (vd http://mailpit:8025) de tra cuu message. */
        private String apiBase = "http://localhost:8025";
        /** SMTP host/port Mailpit de notification-service noi SMTP. */
        private String smtpHost = "localhost";
        private int smtpPort = 1025;
        private String from = "shop@shop-hatd.online";
        /** Timeout goi provider: qua han la UNKNOWN, khong tu coi fail (NOTI-07). */
        private Duration sendTimeout = Duration.ofSeconds(10);
    }

    /** NOTI-06: thu lai loi tam thuat backoff, gioi han 5 lan trong 30 phut. */
    @Getter @Setter
    public static class Retry {
        private int maxAttempts = 5;
        private Duration initialBackoff = Duration.ofMinutes(1);
        private Duration maxBackoff = Duration.ofMinutes(10);
    }

    @Getter @Setter
    public static class Worker {
        /** Chu ky quet hang doi due (ms). */
        private long pollIntervalMs = 2000;
        /** So request moi lan quet — giu nho de khong giu transaction dai. */
        private int batchSize = 100;
        /** So worker gui song song. */
        private int concurrency = 8;
    }
}
