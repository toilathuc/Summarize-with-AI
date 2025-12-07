package integration;

import com.example.summarizer.service.lock.RedisLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.junit.jupiter.Container;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class RedisLockServiceIntegrationTest {

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7.0.5").withExposedPorts(6379);

    private RedisLockService svc;

    @BeforeEach
    void setup() {
        LettuceConnectionFactory cf = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        cf.afterPropertiesSet();

        StringRedisTemplate template = new StringRedisTemplate(cf);

        svc = new RedisLockService(template, "summarizer");
    }

    @Test
    void lock_and_unlock_should_work() {
        boolean locked = svc.tryLock("refresh-job", Duration.ofSeconds(2));

        assertTrue(locked);
        assertTrue(svc.isLocked("refresh-job"));

        svc.unlock("refresh-job");

        assertFalse(svc.isLocked("refresh-job"));
    }

    @Test
    void lock_should_expire_after_ttl() throws InterruptedException {
        svc.tryLock("refresh-job", Duration.ofSeconds(1));

        assertTrue(svc.isLocked("refresh-job"));

        Thread.sleep(1500);

        assertFalse(svc.isLocked("refresh-job"));
    }
}
