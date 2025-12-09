package unit;

import com.example.summarizer.ports.CachePort;
import com.example.summarizer.ports.FeedPort;
import com.example.summarizer.ports.SummarizeUseCase;
import com.example.summarizer.ports.SummaryStorePort;
import com.example.summarizer.service.RefreshCoordinator;
import com.example.summarizer.service.lock.LockService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshCoordinatorTest {

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

        when(registry.timer(anyString())).thenReturn(mock(io.micrometer.core.instrument.Timer.class));

        coordinator = new RefreshCoordinator(orchestrator, store, lock, feed, cacheService, registry, 120);
    }

    @Test
    void manual_should_fail_if_locked() {
        when(lock.tryLock(anyString(), any())).thenReturn(false);

        boolean allowed = coordinator.tryStartManual();

        assertFalse(allowed);
        assertEquals("manual_blocked_already_running", coordinator.getStatus().reason());
    }

    @Test
    void manual_should_start_if_free() {
        when(lock.tryLock(anyString(), any())).thenReturn(true);

        boolean allowed = coordinator.tryStartManual();

        assertTrue(allowed);
        assertEquals("manual_trigger", coordinator.getStatus().reason());
    }

    @Test
    void scheduled_should_skip_if_locked() {
        when(lock.tryLock(anyString(), any())).thenReturn(false);

        boolean result = coordinator.tryStartScheduled();

        assertFalse(result);
        assertEquals("scheduled_skip_already_running", coordinator.getStatus().reason());
    }

    @Test
    void scheduled_should_start_if_free() {
        when(lock.tryLock(anyString(), any())).thenReturn(true);

        boolean result = coordinator.tryStartScheduled();

        assertTrue(result);
        assertEquals("scheduled_trigger", coordinator.getStatus().reason());
    }

    @Test
    void status_running_should_reflect_lock_state() {
        when(lock.isLocked(anyString())).thenReturn(true);

        RefreshCoordinator.RefreshStatus s = coordinator.getStatus();

        assertTrue(s.running());
    }
}
