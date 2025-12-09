package com.example.summarizer.service;

import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.domain.SummaryPayload;
import com.example.summarizer.domain.SummaryResult;
import com.example.summarizer.ports.CachePort;
import com.example.summarizer.ports.FeedPort;
import com.example.summarizer.ports.SummarizeUseCase;
import com.example.summarizer.ports.SummaryStorePort;
import com.example.summarizer.service.lock.LockService;
import com.example.summarizer.utils.PayloadToMapUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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
    private final CachePort cacheService;
    private final Duration lockTtl;
    private final Timer refreshTimer;
    private final AtomicInteger refreshRunningGauge;
    private final ScheduledExecutorService lockRefresher =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "refresh-lock-refresher");
                t.setDaemon(true);
                return t;
            });

    // ====== STATE ======
    private volatile Instant lastRunAt = Instant.EPOCH;
    private volatile String lastReason = "never_run";
    private volatile String currentCorrelationId = null;

    public RefreshCoordinator(
            SummarizeUseCase orchestrator,
            SummaryStorePort summaryStore,
            LockService lockService,
            FeedPort feedPort,
            CachePort cacheService,
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

    /**
     * Initialize the coordinator.
     * Note: We do NOT clear stale locks here anymore to support multiple instances.
     * We rely on the lock TTL to expire naturally if a previous instance crashed.
     */
    @PostConstruct
    public void init() {
        if (lockService.isLocked(REFRESH_LOCK)) {
            log.info("ℹ️ Refresh lock is currently held. Relying on TTL to expire it if stale.");
        }
        log.info("✅ RefreshCoordinator initialized");
    }

    @PreDestroy
    public void tearDown() {
        lockRefresher.shutdown();
    }

    // ========================= STATUS API =========================
    public RefreshStatus getStatus() {

        boolean running = lockService.isLocked(REFRESH_LOCK);

        return new RefreshStatus(
                running,
                lastRunAt != null ? lastRunAt : Instant.EPOCH,
                lastReason != null ? lastReason : "unknown",
                currentCorrelationId
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
        currentCorrelationId = correlationId;

        ScheduledFuture<?> renewal = scheduleLockRenewal();

        try {
            log.info("🔥 REFRESH STARTED — top={}, cid={}", top, correlationId);

            // FEED
            List<FeedArticle> articles = feedPort.fetchLatest(top);
            log.info("📥 Fetched {} articles from Techmeme", articles.size());

            // SUMMARIZE
            List<SummaryResult> summaries = orchestrator.summarize(articles);
            log.info("🧠 Summarized {} articles", summaries.size());

            Map<String, Object> extras = Map.of(
                    "last_updated", OffsetDateTime.now().toString(),
                    "correlation_id", correlationId
            );

            // STORE
            Path out = summaryStore.save(summaries, extras);
            log.info("🧾 Saved {} summaries into SummaryStore", summaries.size());

            // Invalidate caches
            cacheService.evictSummaries();
            cacheService.evictFeed(top);

            // Insert summaries into cache
            SummaryPayload payload = new SummaryPayload(summaries, extras);
            cacheService.putSummaries(PayloadToMapUtils.convertPayloadToMap(payload));
            log.info("🗃 Updated summaries cache");

            // Insert each summary-result into cache
            for (SummaryResult result : summaries) {
                cacheService.putSummaryResult(result);
            }
            log.info("🗃 Updated summaries and summary-results caches");

            // STATE
            lastRunAt = Instant.now();
            lastReason = "success";
            log.info("✅ REFRESH COMPLETED — cid={}, at={}", correlationId, lastRunAt);

            return CompletableFuture.completedFuture(out);

        } catch (Exception ex) {
            log.error("❌ REFRESH FAILED — cid={}", correlationId, ex);
            lastReason = "error";
            return CompletableFuture.failedFuture(ex);

        } finally {
            lockService.unlock(REFRESH_LOCK);
            refreshRunningGauge.set(0);
            renewal.cancel(true);
            currentCorrelationId = null;
            sample.stop(refreshTimer);
        }
    }

    private ScheduledFuture<?> scheduleLockRenewal() {
        // Refresh the lock before TTL expires to avoid double-runs on long jobs.
        long intervalSeconds = Math.max(5, lockTtl.getSeconds() / 3);
        return lockRefresher.scheduleAtFixedRate(() -> {
            try {
                lockService.extendLock(REFRESH_LOCK, lockTtl);
            } catch (Exception ex) {
                log.warn("⚠️ Failed to extend refresh lock: {}", ex.getMessage());
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    // ====== RECORD ======
    public record RefreshStatus(boolean running, Instant lastRunAt, String reason, String correlationId) {}
}
