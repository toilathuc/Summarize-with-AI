package com.example.summarizer.cache;

import java.time.Duration;
import java.util.Map;

public interface KeyValueCacheClient {
    String get(String key);
    void set(String key, String value, Duration ttl);
    void delete(String key);
    Map<String, String> hGetAll(String key);
    void hPutAll(String key, Map<String, String> values, Duration ttl);
}
