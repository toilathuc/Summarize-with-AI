package com.example.summarizer.service;

import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.domain.SummaryResult;
import com.example.summarizer.ports.FeedPort;
import com.example.summarizer.ports.SummarizeUseCase;
import com.example.summarizer.ports.SummaryStorePort;
import com.example.summarizer.service.lock.LockService;
import com.example.summarizer.utils.CacheUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RefreshCoordinator {

    private static final Logger log = LoggerFactory.getLogger(RefreshCoordinator.class);

    // Redis key
    private static final String REFRESH_LOCK = "refresh-job";

    private final SummarizeUseCase orchestrator;
    private final SummaryStorePort summaryStore;
    private final LockService lockService;
    private final FeedPort feedPort;
    private final NewsCacheService cacheService;
    private final Duration lockTtl;
    private final Timer refreshTimer;
    private final AtomicInteger refreshRunningGauge;

    // ====== STATE ======
    private volatile Instant lastRunAt = Instant.EPOCH;
    private volatile String lastReason = "never_run";

    public RefreshCoordinator(
            SummarizeUseCase orchestrator,
            SummaryStorePort summaryStore,
            LockService lockService,
            FeedPort feedPort,
            NewsCacheService cacheService,
            MeterRegistry registry,
            @Value("${refresh.lock.ttl.seconds:30}") long lockTtlSeconds
    ) {
        this.orchestrator = orchestrator;
        this.summaryStore = summaryStore;
        this.lockService = lockService;
        this.feedPort = feedPort;
        this.cacheService = cacheService;
        this.lockTtl = Duration.ofSeconds(Math.max(30, lockTtlSeconds));
        this.refreshRunningGauge = new AtomicInteger(0);
        this.refreshTimer = registry.timer("summarizer_refresh_duration_seconds");
        registry.gauge("summarizer_refresh_running", refreshRunningGauge);
    }

    // ========================= STATUS API =========================
    public RefreshStatus getStatus() {

        boolean running = lockService.isLocked(REFRESH_LOCK);

        return new RefreshStatus(
                running,
                lastRunAt != null ? lastRunAt : Instant.EPOCH,
                lastReason != null ? lastReason : "unknown"
        );
    }

    // ========================= START MANUAL =======================
    /**
     * Manual refresh: fail fast nếu đang chạy.
     */
    public boolean tryStartManual() {

        boolean locked = lockService.tryLock(
                REFRESH_LOCK,
                lockTtl
        );

        if (!locked) {
            lastReason = "manual_blocked_already_running";
            log.warn("⚠️ Manual refresh blocked — an existing refresh is running");
            return false;
        }

        lastReason = "manual_trigger";
        return true;
    }

    // ========================= START SCHEDULED ====================
    /**
     * Scheduled refresh: nếu đang chạy thì skip, KHÔNG chặn.
     */
    public boolean tryStartScheduled() {

        boolean locked = lockService.tryLock(
                REFRESH_LOCK,
                lockTtl
        );

        if (!locked) {
            lastReason = "scheduled_skip_already_running";
            log.info("⏭ Scheduled refresh skipped — a previous job is still running");
            return false;
        }

        lastReason = "scheduled_trigger";
        return true;
    }

    // ========================= ASYNC PIPELINE =====================
    @Async
    public CompletableFuture<Path> runAsyncRefresh(int top, String correlationId) {

        Timer.Sample sample = Timer.start();
        refreshRunningGauge.set(1);

        try {
            log.info("🔥 REFRESH STARTED — top={}, cid={}", top, correlationId);

            // FEED
            List<FeedArticle> articles = feedPort.fetchLatest(top);
            log.info("📥 Fetched {} articles from Techmeme", articles.size());

            // CACHE LOAD
            Map<String, SummaryResult> cache = CacheUtils.loadSummaryCache(summaryStore, log);

            // SUMMARIZE
            List<SummaryResult> summaries = orchestrator.summarize(articles, cache);
            log.info("🧠 Summarized {} articles", summaries.size());

            // STORE
            Path out = summaryStore.save(
                    summaries,
                    Map.of(
                            "last_updated", OffsetDateTime.now().toString(),
                            "correlation_id", correlationId
                    )
            );
            log.info("🧾 Saved {} summaries into SummaryStore", summaries.size());

            // Invalidate caches
            cacheService.evictSummaries();
            cacheService.evictFeed(top);

            // STATE
            lastRunAt = Instant.now();
            lastReason = "success";
            log.info("✅ REFRESH COMPLETED — cid={}, at={}", correlationId, lastRunAt);

            return CompletableFuture.completedFuture(out);

        } catch (Exception ex) {
            log.error("❌ REFRESH FAILED — cid=" + correlationId, ex);
            lastReason = "error";
            return CompletableFuture.failedFuture(ex);

        } finally {
            lockService.unlock(REFRESH_LOCK);
            refreshRunningGauge.set(0);
            sample.stop(refreshTimer);
        }
    }

    // ====== RECORD ======
    public record RefreshStatus(boolean running, Instant lastRunAt, String reason) {}
}
