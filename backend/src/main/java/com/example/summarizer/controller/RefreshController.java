package com.example.summarizer.controller;

import com.example.summarizer.service.RefreshCoordinator;
import com.example.summarizer.service.ratelimit.RateLimitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;

@RestController
public class RefreshController {

    private final RefreshCoordinator coordinator;
    private final RateLimitService rateLimit;

    public RefreshController(RefreshCoordinator coordinator,
                             RateLimitService rateLimit) {
        this.coordinator = coordinator;
        this.rateLimit = rateLimit;
    }

    @PostMapping("/api/refresh")
    public ResponseEntity<?> triggerManual(
            @RequestParam(name = "top", defaultValue = "20") int top,
            @RequestHeader(name = "X-Correlation-ID", required = false) String incomingId,
            HttpServletRequest request
    ) {

        // ====== GEN CID ======
        String cid = (incomingId != null && !incomingId.isBlank())
                ? incomingId
                : UUID.randomUUID().toString();

        // ====== TRY START MANUAL ======
        if (!coordinator.tryStartManual()) {

            var status = coordinator.getStatus();

            return ResponseEntity.status(202).body(Map.of(
                    "status", "running",
                    "message", "Refresh already in progress",
                    "running", true,
                    "reason", status.reason(),
                    "lastRunAt", status.lastRunAt(),
                    "correlation_id_running_job", cid
            ));
        }

        // ====== START NEW REFRESH (ASYNC) ======
        coordinator.runAsyncRefresh(top, cid)
                .thenAccept(path ->
                        System.out.println("📦 MANUAL refresh completed, output=" + path)
                )
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });

        return ResponseEntity.ok(Map.of(
                "status", "started",
                "running", true,
                "correlation_id", cid,
                "top", top
        ));
    }

    @GetMapping("/api/refresh/status")
    public ResponseEntity<?> getStatus() {
        var s = coordinator.getStatus();
        return ResponseEntity.ok(Map.of(
                "running", s.running(),
                "lastRunAt", s.lastRunAt(),
                "reason", s.reason()
        ));
    }
}
