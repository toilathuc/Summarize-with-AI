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

        // ====== RATE LIMIT PER-IP (3 lần / 60s) ======
        String ip = request.getRemoteAddr();
        if (!rateLimit.allow("refresh:" + ip, 3, 60)) {
            return ResponseEntity.status(429).body(Map.of(
                    "status", "rate_limited",
                    "message", "Too many refresh attempts from your IP. Try again later.",
                    "ip", ip,
                    "limit", 3,
                    "window_seconds", 60
            ));
        }

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

        // ====== START NEW REFRESH ======
        coordinator.runAsyncRefresh(top, cid);

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
