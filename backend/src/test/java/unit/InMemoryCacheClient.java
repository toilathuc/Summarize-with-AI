package unit;

import com.example.summarizer.cache.KeyValueCacheClient;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCacheClient implements KeyValueCacheClient {
    private final Map<String, String> kv = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> hashes = new ConcurrentHashMap<>();

    @Override
    public String get(String key) {
        return kv.get(key);
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        kv.put(key, value);
    }

    @Override
    public void delete(String key) {
        kv.remove(key);
        hashes.remove(key);
    }

    @Override
    public Map<String, String> hGetAll(String key) {
        Map<String, String> map = hashes.get(key);
        if (map == null) return Map.of();
        return Collections.unmodifiableMap(map);
    }

    @Override
    public void hPutAll(String key, Map<String, String> values, Duration ttl) {
        hashes.put(key, new HashMap<>(values));
    }
}
