package com.dtthuan3.ecommerce.notificationservice.service;

import com.dtthuan3.ecommerce.notificationservice.config.NotificationProperties;
import com.dtthuan3.ecommerce.notificationservice.entity.NotificationRequest;
import com.dtthuan3.ecommerce.notificationservice.repository.NotificationRequestRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
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

    /** Mot vong quet: lay batch due (LIMIT trong DB) va dispatch song song cho sendPool.
     *  Kem recovery: request SENDING "ma" (pod crash giu transaction) duoc
     *  chuyen lai QUEUED de xu ly lai — chong kec vinh vien.
     *  BACKPRESSURE: doi batch nay xu ly xong (CompletableFuture.allOf) truoc khi
     *  poll batch sau — khong submit 400 tx/giay vao 5 connection hikari lam
     *  RAM phinh (da gay OOMKilled 4 lan: 384/512/768/1024Mi). */
    int pollOnce() {
        int processed = recoverStaleSending();
        List<NotificationRequest> due = requests.findDueBatch(Instant.now(),
                PageRequest.of(0, props.getWorker().getBatchSize()));
        if (due.isEmpty()) return processed;
        log.debug("Quet duoc {} request due", due.size());
        List<java.util.concurrent.CompletableFuture<Void>> futures = new java.util.ArrayList<>(due.size());
        for (NotificationRequest request : due) {
            futures.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    processor.processOne(request.getId());
                } catch (Exception e) {
                    log.error("Xu ly request {} loi: {}", request.getId(), e.getMessage(), e);
                }
            }, sendPool));
        }
        // Cho batch xong het (co timeout de khong keo vinh vien) roi moi poll tiep —
        // han che so transaction dang mo dong thoi = han che RAM
        try {
            java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                    .get(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Cho batch xong bi gian doan: {}", e.getMessage());
        }
        return processed + due.size();
    }

    /** Request SENDING khong nhat xu sau 5 phut = pod da chet giu no.
     *  Chuyen ve QUEUED (worker se pick o vong sau) — an toan vi send SMTP
     *  kem businessKey, Mailpit dedup theo header. */
    private int recoverStaleSending() {
        Instant staleBefore = Instant.now().minus(Duration.ofMinutes(5));
        List<NotificationRequest> stale = requests.findStaleSending(staleBefore,
                PageRequest.of(0, props.getWorker().getBatchSize()));
        if (stale.isEmpty()) return 0;
        log.warn("Recovery {} request SENDING mai (pod restart giu transaction)", stale.size());
        for (NotificationRequest request : stale) {
            request.setStatus("QUEUED");
            request.setNextAttemptAt(Instant.now());
            requests.save(request);
        }
        return stale.size();
    }
}
