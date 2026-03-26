package com.example.summarizer.config;

import com.example.summarizer.service.ratelimit.RateLimitService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rl;
    private final int globalLimit;
    private final int refreshLimit;
    private final int windowSeconds;
    private final MeterRegistry registry;
    private final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    public RateLimitFilter(
            RateLimitService rl,
            @Value("${redis.rate-limit.global-limit:60}") int globalLimit,
            @Value("${redis.rate-limit.refresh-limit:5}") int refreshLimit,
            @Value("${redis.rate-limit.window-seconds:60}") int windowSeconds,
            MeterRegistry registry
    ) {
        this.rl = rl;
        this.globalLimit = globalLimit;
        this.refreshLimit = refreshLimit;
        this.windowSeconds = windowSeconds;
        this.registry = registry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String ip = req.getRemoteAddr();
        String path = req.getRequestURI();

        if (path.startsWith("/api/refresh")) {
            String refreshKey = "ip:" + ip + ":refresh";

            if (!rl.allow(refreshKey, refreshLimit, windowSeconds)) {
                logger.warn("Rate limit exceeded for refresh from IP {}", ip);
                long retryAfter = rl.getRetryAfter(refreshKey);

                recordRateLimit("refresh", "blocked");

                res.setStatus(429);
                res.setContentType("application/json");
                res.getWriter().write(
                        "{\"status\":\"rate_limited\",\"scope\":\"refresh\", \"retry_after_seconds\":" + retryAfter + "}"
                );
                return;
            }

            recordRateLimit("refresh", "allowed");
        }

        String globalKey = "ip:" + ip + ":all";

        if (!rl.allow(globalKey, globalLimit, windowSeconds)) {
            logger.warn("Global rate limit exceeded from IP {}", ip);
            long retryAfter = rl.getRetryAfter(globalKey);

            recordRateLimit("global", "blocked");

            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write(
                    "{\"status\":\"rate_limited\",\"scope\":\"global\", \"retry_after_seconds\":" + retryAfter + "}"
            );
            return;
        }

        recordRateLimit("global", "allowed");
        chain.doFilter(req, res);
    }

    private void recordRateLimit(String scope, String decision) {
        if (registry == null) return;
        registry.counter("summarizer_rate_limit_total",
                "scope", scope,
                "decision", decision).increment();
    }
}
