package com.example.summarizer.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RedisCacheClient implements KeyValueCacheClient {

    private final StringRedisTemplate redis;

    public RedisCacheClient(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }

    @Override
    public void delete(String key) {
        redis.delete(key);
    }

    @Override
    public Map<String, String> hGetAll(String key) {
        Map<Object, Object> raw = redis.opsForHash().entries(key);
        if (raw == null || raw.isEmpty()) return Map.of();
        return raw.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> e.getValue() == null ? "" : e.getValue().toString()
                ));
    }

    @Override
    public void hPutAll(String key, Map<String, String> values, Duration ttl) {
        if (values == null || values.isEmpty()) {
            redis.delete(key);
            return;
        }
        redis.opsForHash().putAll(key, new HashMap<>(values));
        redis.expire(key, ttl);
    }
}
