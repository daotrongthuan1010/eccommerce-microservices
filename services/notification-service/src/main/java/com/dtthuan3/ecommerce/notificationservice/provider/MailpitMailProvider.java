package com.dtthuan3.ecommerce.notificationservice.provider;

import com.dtthuan3.ecommerce.notificationservice.config.NotificationProperties;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Mail provider gia lap = Mailpit (SMTP :1025, API :8025). Docs ch.15: Mailpit la SMTP kiem tra rieng.
 * - Gui SMTP kem header X-Notification-Business-Key de chong trung/tra vet.
 * - Timeout -> UNKNOWN (co the Mailpit da nhan) — khong tu gui lai ngay (NOTI-07).
 * - 4xx (dia chi sai) -> FAILED_FINAL permanent; 5xx -> FAILED_RETRYABLE.
 * - Mailpit khong co trang thai "delivered" rieng: SMTP 250 = Mailpit da tiep nhan vao inbox
 *   => ghi ACCEPTED; API tra cuu duoc thi nang len DELIVERED (co can cu that).
 */
@Component
public class MailpitMailProvider {

    private static final Logger log = LoggerFactory.getLogger(MailpitMailProvider.class);

    private final NotificationProperties props;
    private final WebClient apiClient;
    private final Session smtpSession;

    public MailpitMailProvider(NotificationProperties props) {
        this.props = props;
        this.apiClient = WebClient.builder()
                .baseUrl(props.getMail().getApiBase())
                .build();
        Properties smtp = new Properties();
        smtp.put("mail.smtp.host", props.getMail().getSmtpHost());
        smtp.put("mail.smtp.port", String.valueOf(props.getMail().getSmtpPort()));
        smtp.put("mail.smtp.connectiontimeout", String.valueOf(props.getMail().getSendTimeout().toMillis()));
        smtp.put("mail.smtp.timeout", String.valueOf(props.getMail().getSendTimeout().toMillis()));
        smtp.put("mail.smtp.writetimeout", String.valueOf(props.getMail().getSendTimeout().toMillis()));
        smtp.put("mail.smtp.auth", "false");
        this.smtpSession = Session.getInstance(smtp, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return null; // Mailpit dev khong auth
            }
        });
    }

    /**
     * Gui mot mail qua Mailpit SMTP. businessKey di kem header de doi chieu neu can thu lai.
     */
    public SendResult send(String businessKey, String to, String subject, String body) {
        try {
            MimeMessage message = new MimeMessage(smtpSession);
            message.setFrom(new InternetAddress(props.getMail().getFrom()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
            message.setSubject(subject == null ? "" : subject, "UTF-8");
            message.setHeader("X-Notification-Business-Key", businessKey);
            message.setHeader("X-Notification-Id", UUID.randomUUID().toString());
            message.setSentDate(new Date());
            message.setText(body, "UTF-8");
            Transport.send(message);
            // SMTP 250 tu Mailpit = da tiep nhan vao inbox => ACCEPTED.
            // Mailpit la inbox cuoi cung nen co the xem la DELIVERED khi API tra cuu duoc.
            return SendResult.accepted(null);
        } catch (jakarta.mail.internet.AddressException e) {
            // Dia chi sai: permanent, khong thu lai (NOTI-06)
            log.warn("[MAIL] Dia chi khong hop le: {} — {}", to, e.getMessage());
            return SendResult.permanent("INVALID_ADDRESS", e.getMessage());
        } catch (jakarta.mail.MessagingException e) {
            // Loi ket noi/SMTP: co the Mailpit down hoac da nhan roi mat ACK -> UNKNOWN
            log.warn("[MAIL] SMTP loi (co the da nhan): {}", e.getMessage());
            return SendResult.unknown("SMTP_ERROR", e.getMessage());
        }
    }

    /**
     * Tra cuu Mailpit API theo businessKey de giai UNKNOWN (NOTI-07): da nhan thi nang DELIVERED.
     */
    public SendResult lookupByBusinessKey(String businessKey) {
        try {
            String response = apiClient.get()
                    .uri(uri -> uri.path("/api/v1/search")
                            .queryParam("query", "X-Notification-Business-Key:\"" + businessKey + "\"")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(props.getMail().getSendTimeout());
            boolean found = response != null && response.contains("\"total\":");
            // Mailpit search tra {"total":N,"messages":[...]}
            if (found && !response.contains("\"total\":0")) {
                return SendResult.delivered(null);
            }
            return SendResult.retryable("NOT_FOUND", "Mailpit khong thay businessKey=" + businessKey);
        } catch (WebClientResponseException e) {
            log.warn("[MAIL] Mailpit API tra ve {}: {}", e.getStatusCode(), e.getMessage());
            return SendResult.retryable("API_ERROR", e.getMessage());
        } catch (Exception e) {
            if (e instanceof java.util.concurrent.CancellationException || e.getCause() instanceof TimeoutException) {
                return SendResult.unknown("API_TIMEOUT", e.getMessage());
            }
            return SendResult.retryable("API_ERROR", e.getMessage());
        }
    }
}
