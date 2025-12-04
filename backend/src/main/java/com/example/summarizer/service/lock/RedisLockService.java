package com.example.summarizer.service.lock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Service
public class RedisLockService implements LockService {

    private final StringRedisTemplate redis;
    private final String prefix;

    public RedisLockService(
            StringRedisTemplate redis,
            @Value("${redis.key-prefix:summarizer}") String prefix) {
        this.redis = redis;
        this.prefix = prefix;
    }

    private String key(String raw) {
        return prefix + ":lock:" + raw;
    }

    // mỗi lock có một token để tránh unlock nhầm
    private static final ThreadLocal<String> tokenHolder = new ThreadLocal<>();

    @Override
    public boolean tryLock(String rawKey, Duration ttl) {
        String redisKey = key(rawKey);
        String token = UUID.randomUUID().toString();

        Boolean success = redis.execute((RedisCallback<Boolean>) con ->
                con.stringCommands().set(
                        redisKey.getBytes(StandardCharsets.UTF_8),
                        token.getBytes(StandardCharsets.UTF_8),
                        Expiration.from(ttl),
                        RedisStringCommands.SetOption.ifAbsent()
                )
        );

        if (Boolean.TRUE.equals(success)) {
            tokenHolder.set(token);
            return true;
        }

        return false;
    }

    @Override
    public void unlock(String rawKey) {
        String redisKey = key(rawKey);
        String token = tokenHolder.get();

        String current = redis.opsForValue().get(redisKey);

        // chỉ xóa lock nếu đúng process sở hữu
        if (token != null && token.equals(current)) {
            redis.delete(redisKey);
        }

        tokenHolder.remove();
    }

    @Override
    public boolean isLocked(String rawKey) {
        return redis.hasKey(key(rawKey));
    }
}
