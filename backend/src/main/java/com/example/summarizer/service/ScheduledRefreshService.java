package com.example.summarizer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ScheduledRefreshService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledRefreshService.class);

    private final RefreshCoordinator coordinator;

    public ScheduledRefreshService(RefreshCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Scheduled(fixedDelayString = "${refresh.interval.ms}")
    public void runScheduled() {

        // 🔥 Job đang chạy → skip
        if (!coordinator.tryStartScheduled()) {
            log.info("⏭ Scheduled refresh skipped (busy)");
            return;
        }

        // 🔥 Start async job
        String cid = "scheduler-" + System.currentTimeMillis();

        CompletableFuture<Void> future =
                coordinator.runAsyncRefresh(20, cid)
                        .thenAccept(path ->
                                log.info("📦 Scheduled job completed, output={}", path)
                        )
                        .exceptionally(ex -> {
                            log.error("❌ Scheduled job failed", ex);
                            return null;
                        });

        // KHÔNG được block (no get()!)
    }
}
