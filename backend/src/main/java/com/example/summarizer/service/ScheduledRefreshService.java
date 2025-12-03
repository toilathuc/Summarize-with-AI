package com.example.summarizer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledRefreshService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledRefreshService.class);

    private final RefreshCoordinator coordinator;
    private final long interval;

    public ScheduledRefreshService(
            RefreshCoordinator coordinator,
            @org.springframework.beans.factory.annotation.Value("${refresh.interval.ms:-1}") long interval
    ) {
        this.coordinator = coordinator;
        this.interval = interval;
    }

    @Scheduled(fixedDelayString = "${refresh.interval.ms}")
    public void runScheduled() {

        // 🔥 Job đang chạy → skip
        if (!coordinator.tryStartScheduled()) {
            log.info("⏭ Scheduled refresh skipped (busy)");
            return;
        }

        // 🔥 Bắt đầu chạy job
        coordinator.runAsyncRefresh(20, "scheduler-" + System.currentTimeMillis());
    }
}
