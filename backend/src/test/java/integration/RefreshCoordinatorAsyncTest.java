package integration;

import com.example.summarizer.ports.FeedPort;
import com.example.summarizer.ports.SummarizeUseCase;
import com.example.summarizer.ports.SummaryStorePort;
import com.example.summarizer.service.RefreshCoordinator;
import com.example.summarizer.service.lock.LockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class RefreshCoordinatorAsyncTest {

    private SummarizeUseCase orchestrator;
    private SummaryStorePort store;
    private LockService lock;
    private FeedPort feed;
    private RefreshCoordinator coordinator;

    @BeforeEach
    void setup() {
        orchestrator = mock(SummarizeUseCase.class);
        store = mock(SummaryStorePort.class);
        lock = mock(LockService.class);
        feed = mock(FeedPort.class);

        coordinator = new RefreshCoordinator(orchestrator, store, lock, feed);
    }

    @Test
    void async_refresh_should_unlock_after_finish() throws Exception {
        when(lock.tryLock(anyString(), any())).thenReturn(true);
        when(feed.fetchLatest(anyInt())).thenReturn(List.of());
        when(orchestrator.summarize(anyList(), anyMap())).thenReturn(List.of());
        when(store.save(anyList(), anyMap())).thenReturn(Path.of("output.json"));

        CompletableFuture<Path> future =
                coordinator.runAsyncRefresh(20, "cid-001");

        Path result = future.get();

        assertEquals("output.json", result.toString());
        verify(lock).unlock("refresh-job");
    }
}
