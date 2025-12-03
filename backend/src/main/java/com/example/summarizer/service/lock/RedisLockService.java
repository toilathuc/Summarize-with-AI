package com.example.summarizer.service.lock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class RedisLockService implements LockService {

    private final StringRedisTemplate redis;
    private final String prefix;

    public RedisLockService(
            StringRedisTemplate redis,
            @Value("${redis.key-prefix:summarizer}") String prefix
    ) {
        this.redis = redis;
        this.prefix = prefix;
    }

    private String key(String raw) {
        return prefix + ":lock:" + raw;
    }

    @Override
    public boolean tryLock(String rawKey, Duration ttl) {
        String redisKey = key(rawKey);

        // 1) SETNX – tạo lock
        Boolean ok = redis.opsForValue().setIfAbsent(
                redisKey,
                String.valueOf(Instant.now().toEpochMilli())
        );

        // Nếu không lock được → có lock cũ → return false
        if (!Boolean.TRUE.equals(ok)) {
            return false;
        }

        // 2) ĐẢM BẢO TTL luôn tồn tại
        Boolean expireOk = redis.expire(redisKey, ttl);

        if (!Boolean.TRUE.equals(expireOk)) {
            // Nếu expire fail → xóa lock để tránh treo
            redis.delete(redisKey);
            return false;
        }

        return true;
    }

    @Override
    public void unlock(String rawKey) {
        redis.delete(key(rawKey));
    }

    @Override
    public boolean isLocked(String rawKey) {
        return redis.hasKey(key(rawKey));
    }
}
