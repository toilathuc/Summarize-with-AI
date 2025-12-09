package integration;

import com.example.summarizer.ports.CachePort;
import com.example.summarizer.ports.FeedPort;
import com.example.summarizer.ports.SummarizeUseCase;
import com.example.summarizer.ports.SummaryStorePort;
import com.example.summarizer.service.RefreshCoordinator;
import com.example.summarizer.service.lock.LockService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import com.example.summarizer.utils.PayloadToMapUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class RefreshCoordinatorAsyncTest {

    private SummarizeUseCase orchestrator;
    private SummaryStorePort store;
    private LockService lock;
    private FeedPort feed;
    private CachePort cacheService;
    private MeterRegistry registry;

    private RefreshCoordinator coordinator;

    @BeforeEach
    void setup() {
        orchestrator = mock(SummarizeUseCase.class);
        store = mock(SummaryStorePort.class);
        lock = mock(LockService.class);
        feed = mock(FeedPort.class);
        cacheService = mock(CachePort.class);
        registry = mock(MeterRegistry.class);

        // mock registry.timer() không null
        when(registry.timer(anyString())).thenReturn(mock(io.micrometer.core.instrument.Timer.class));

        coordinator = new RefreshCoordinator(orchestrator, store, lock, feed, cacheService, registry, 120);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void async_refresh_should_unlock_after_finish() throws Exception {

        when(lock.tryLock(anyString(), any())).thenReturn(true);

        when(feed.fetchLatest(anyInt())).thenReturn(List.of());
        when(orchestrator.summarize(anyList())).thenReturn(List.of());

        when(store.save(anyList(), anyMap())).thenReturn(Path.of("output.json"));

        // static mock for PayloadToMapUtils.convertPayloadToMap()
        try (MockedStatic<PayloadToMapUtils> payloadUtilsMock = mockStatic(PayloadToMapUtils.class)) {
            payloadUtilsMock.when(() -> PayloadToMapUtils.convertPayloadToMap(any()))
                    .thenReturn(Map.of());

            CompletableFuture<Path> future =
                    coordinator.runAsyncRefresh(20, "cid-001");

            Path result = future.get();

            assertEquals("output.json", result.toString());
            verify(lock).unlock("refresh-job");
            verify(cacheService).evictSummaries();
        }
    }
}
