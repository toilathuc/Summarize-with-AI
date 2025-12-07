package integration;

import com.example.summarizer.domain.SummaryResult;
import com.example.summarizer.ports.FeedPort;
import com.example.summarizer.ports.SummarizeUseCase;
import com.example.summarizer.ports.SummaryStorePort;
import com.example.summarizer.service.NewsCacheService;
import com.example.summarizer.service.RefreshCoordinator;
import com.example.summarizer.service.lock.LockService;
import com.example.summarizer.utils.CacheUtils;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class RefreshCoordinatorAsyncTest {

    private SummarizeUseCase orchestrator;
    private SummaryStorePort store;
    private LockService lock;
    private FeedPort feed;
    private NewsCacheService cacheService;
    private MeterRegistry registry;

    private RefreshCoordinator coordinator;
    private MockedStatic<CacheUtils> cacheUtilsMock;

    @BeforeEach
    void setup() {
        orchestrator = mock(SummarizeUseCase.class);
        store = mock(SummaryStorePort.class);
        lock = mock(LockService.class);
        feed = mock(FeedPort.class);
        cacheService = mock(NewsCacheService.class);
        registry = mock(MeterRegistry.class);

        // mock registry.timer() không null
        when(registry.timer(anyString())).thenReturn(mock(io.micrometer.core.instrument.Timer.class));

        coordinator = new RefreshCoordinator(orchestrator, store, lock, feed, cacheService, registry, 120);

        // mock static CacheUtils
        cacheUtilsMock = mockStatic(CacheUtils.class);
    }

    @AfterEach
    void tearDown() {
        cacheUtilsMock.close();
    }

    @Test
    void async_refresh_should_unlock_after_finish() throws Exception {

        when(lock.tryLock(anyString(), any())).thenReturn(true);

        when(feed.fetchLatest(anyInt())).thenReturn(List.of());
        when(orchestrator.summarize(anyList(), anyMap())).thenReturn(List.of());

        when(store.save(anyList(), anyMap())).thenReturn(Path.of("output.json"));

        // static mock for CacheUtils.loadSummaryCache()
        cacheUtilsMock.when(() -> CacheUtils.loadSummaryCache(any(), any()))
                .thenReturn(Map.of());

        CompletableFuture<Path> future =
                coordinator.runAsyncRefresh(20, "cid-001");

        Path result = future.get();

        assertEquals("output.json", result.toString());
        verify(lock).unlock("refresh-job");
        verify(cacheService).evictSummaries();
    }
}
