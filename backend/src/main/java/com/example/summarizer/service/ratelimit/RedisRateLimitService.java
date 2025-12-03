package com.example.summarizer.service.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisRateLimitService implements RateLimitService {

    private final StringRedisTemplate redis;
    private final String prefix;

    public RedisRateLimitService(StringRedisTemplate redis,
                                 @Value("${redis.key-prefix:summarizer}") String prefix) {
        this.redis = redis;
        this.prefix = prefix;
    }

    private String key(String raw) {
        return prefix + ":rl:" + raw;
    }

    @Override
    public boolean allow(String rawKey, int limit, int seconds) {
        String k = key(rawKey);

        Long count = redis.opsForValue().increment(k);

        if (count != null && count == 1L) {
            redis.expire(k, Duration.ofSeconds(seconds));
        }

        return count != null && count <= limit;
    }
}
