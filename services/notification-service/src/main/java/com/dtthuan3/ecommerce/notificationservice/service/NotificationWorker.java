package com.dtthuan3.ecommerce.notificationservice.service;

import com.dtthuan3.ecommerce.notificationservice.config.NotificationProperties;
import com.dtthuan3.ecommerce.notificationservice.entity.NotificationRequest;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationRequestRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Worker nen: quet request due (QUEUED/FAILED_RETRYABLE) va dispatch cho
 * NotificationRequestProcessor (bean rieng — @Transactional REQUIRES_NEW
 * hoat dong dung vi goi qua proxy). NOTI-01: gui nam ngoai transaction DB.
 */
@Component
public class NotificationWorker {

    private static final Logger log = LoggerFactory.getLogger(NotificationWorker.class);

    private final NotificationRequestRepository requests;
    private final NotificationRequestProcessor processor;
    private final NotificationProperties props;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread pollerThread;
    private ExecutorService sendPool;

    public NotificationWorker(NotificationRequestRepository requests,
                              NotificationRequestProcessor processor,
                              NotificationProperties props) {
        this.requests = requests;
        this.processor = processor;
        this.props = props;
    }

    @PostConstruct
    void start() {
        running.set(true);
        sendPool = Executors.newFixedThreadPool(props.getWorker().getConcurrency());
        pollerThread = new Thread(this::pollLoop, "notification-worker");
        pollerThread.setDaemon(true);
        pollerThread.start();
        log.info("NotificationWorker bat dau: batch={} concurrency={} poll={}ms",
                props.getWorker().getBatchSize(), props.getWorker().getConcurrency(),
                props.getWorker().getPollIntervalMs());
    }

    @PreDestroy
    void stop() throws InterruptedException {
        running.set(false);
        if (pollerThread != null) pollerThread.join(5000);
        if (sendPool != null) {
            sendPool.shutdown();
            sendPool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private void pollLoop() {
        while (running.get()) {
            try {
                int processed = pollOnce();
                if (processed == 0) {
                    // Khong co gi due — ngu mot chu ky thay vi quay vong an CPU
                    Thread.sleep(props.getWorker().getPollIntervalMs());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("Worker poll loop loi: {}", e.getMessage(), e);
                try { Thread.sleep(props.getWorker().getPollIntervalMs()); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    /** Mot vong quet: lay batch due (LIMIT trong DB) va dispatch song song cho sendPool. */
    int pollOnce() {
        List<NotificationRequest> due = requests.findDueBatch(Instant.now(),
                PageRequest.of(0, props.getWorker().getBatchSize()));
        if (due.isEmpty()) return 0;
        log.debug("Quet duoc {} request due", due.size());
        for (NotificationRequest request : due) {
            sendPool.submit(() -> {
                try {
                    processor.processOne(request.getId());
                } catch (Exception e) {
                    log.error("Xu ly request {} loi: {}", request.getId(), e.getMessage(), e);
                }
            });
        }
        return due.size();
    }
}
