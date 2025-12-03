package com.example.summarizer.service.lock;

import java.time.Duration;

public interface LockService {
    boolean tryLock(String key, Duration ttl);
    void unlock(String key);
    boolean isLocked(String key);
}
