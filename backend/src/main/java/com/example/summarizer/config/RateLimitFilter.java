package com.example.summarizer.config;

import com.example.summarizer.service.ratelimit.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    public RateLimitFilter(
            RateLimitService rl,
            @Value("${redis.rate-limit.global-limit:60}") int globalLimit,
            @Value("${redis.rate-limit.refresh-limit:5}") int refreshLimit,
            @Value("${redis.rate-limit.window-seconds:60}") int windowSeconds
    ) {
        this.rl = rl;
        this.globalLimit = globalLimit;
        this.refreshLimit = refreshLimit;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        String ip = req.getRemoteAddr();
        String path = req.getRequestURI();

        // Rate-limit riêng refresh
        if (path.startsWith("/api/refresh")) {
            if (!rl.allow("ip:" + ip + ":refresh", refreshLimit, windowSeconds)) {
                res.setStatus(429);
                res.setContentType("application/json");
                res.getWriter().write("{\"status\":\"rate_limited\",\"scope\":\"refresh\"}");
                return;
            }
        }

        // Rate-limit toàn app
        if (!rl.allow("ip:" + ip + ":all", globalLimit, windowSeconds)) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"status\":\"rate_limited\",\"scope\":\"global\"}");
            return;
        }

        chain.doFilter(req, res);
    }
}
