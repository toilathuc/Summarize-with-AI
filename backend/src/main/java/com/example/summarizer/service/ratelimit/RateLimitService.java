package com.example.summarizer.service.ratelimit;

public interface RateLimitService {
    boolean allow(String key, int limit, int windowSeconds);
    long getRetryAfter(String key);
}
