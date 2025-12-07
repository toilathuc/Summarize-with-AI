package unit;

import com.example.summarizer.service.ratelimit.RedisRateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class RedisRateLimitServiceTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> ops;
    private RedisRateLimitService svc;

    @BeforeEach
    void setup() {
        redis = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);

        when(redis.opsForValue()).thenReturn(ops);

        svc = new RedisRateLimitService(redis, "prefix");
    }

    @Test
    void allow_should_increment_and_allow_until_limit() {

        when(ops.increment("prefix:rl:test")).thenReturn(1L);

        boolean allowed = svc.allow("test", 5, 60);

        assertTrue(allowed);
    }

    @Test
    void allow_should_deny_when_exceeding_limit() {

        when(ops.increment("prefix:rl:test")).thenReturn(6L);

        boolean allowed = svc.allow("test", 5, 60);

        assertFalse(allowed);
    }
}
