package com.example.summarizer.clients;

import com.example.summarizer.ports.SummarizerPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.UUID;

/**
 * GeminiClient — PRODUCTION version
 * ----------------------------------
 * ✓ Retry thông minh với backoff
 * ✓ Timeout cứng
 * ✓ User-Agent tránh chặn bot
 * ✓ Google format parsing resilient
 * ✓ An toàn cho xử lý song song (virtual threads)
 */
public class GeminiClient implements SummarizerPort {

    private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);

    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    private final String apiKey;
    private final String model;
    private final URI endpoint;
    private final int maxRetries;
    private final String provider;
    private final boolean useApiKeyAsQuery;
    private final Counter externalCallCounter;

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    public GeminiClient(String apiKey, String model, int maxRetries, String endpointUrl,
                        String provider, boolean useApiKeyAsQuery, MeterRegistry registry) {

        this.apiKey = apiKey;
        this.model = model;
        this.maxRetries = Math.max(1, maxRetries);
        this.endpoint = URI.create(endpointUrl);
        this.provider = provider == null || provider.isEmpty() ? "google" : provider;
        this.useApiKeyAsQuery = useApiKeyAsQuery;
        this.externalCallCounter = registry == null ? null
                : registry.counter("summarizer_external_calls_total", "service", "gemini");

        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }


    @Override
    public String generate(String prompt) throws IOException, InterruptedException {

        // MOCK MODE: Trả về kết quả giả lập ngay lập tức
        if ("mock".equalsIgnoreCase(provider)) {
            logger.info("Gemini (MOCK) → Returning fake summary");
            return buildMockPayload(prompt);
        }

        // Default: Google Payload
        String payload = mapper.writeValueAsString(buildGooglePayload(prompt));

        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                                + "Chrome/125.0 Safari/537.36");

        // Google Auth: Query param (default) or Bearer token
        if (useApiKeyAsQuery) {
            String sep = endpoint.getQuery() == null ? "?" : "&";
            rb.uri(URI.create(endpoint + sep + "key=" + apiKey));
        } else {
            rb.uri(endpoint);
            rb.header("Authorization", "Bearer " + apiKey);
        }

        HttpRequest request = rb.POST(HttpRequest.BodyPublishers.ofString(payload)).build();

        // correlation id theo request → info dễ hơn
        String correlationId = UUID.randomUUID().toString().substring(0, 8);

        logger.info("Gemini({}) → Prepared request: provider={}, model={}, endpoint={}",
                correlationId, provider, model, endpoint);

        long backoff = 5000; // Start with 5s backoff
        int attempt = 0;

        while (attempt <= maxRetries) {
            attempt++;

            try {
                logger.info("Gemini({}) attempt {}", correlationId, attempt);

                if (attempt > maxRetries) {
                    logger.error("Gemini({}) exceeded max retries ({})", correlationId, maxRetries);
                    throw new IOException("Gemini exceeded max retries (" + maxRetries + ")");
                }

                incrementExternalCalls();

                HttpResponse<String> resp =
                        http.send(request, HttpResponse.BodyHandlers.ofString());

                int code = resp.statusCode();
                String body = resp.body();

                if (isSuccess(code)) {
                    logger.info("Gemini({}) success ({} chars)", correlationId,
                            body == null ? 0 : body.length());

                    try {
                        return extractGoogleText(body);
                    } catch (Exception ex) {
                        logger.warn("Gemini({}) parse-google failed → using raw", correlationId);
                        return body;
                    }
                }

                if (!isRetryable(code)) {
                    logger.error("Gemini({}) non-retryable {}: {}", correlationId, code, body);
                    throw new IOException("Gemini error " + code + ": " + body);
                }

                logger.warn("Gemini({}) retryable {} → retry {} of {}", correlationId, code, attempt, maxRetries);

                // Nếu gặp 429 (Rate Limit), ngủ hẳn 60s để hồi quota
                if (code == 429) {
                    logger.warn("Gemini({}) hit 429 → Sleeping 60s to cool down...", correlationId);
                    Thread.sleep(60000);
                    // Reset backoff để không tăng thêm nữa
                    backoff = 5000; 
                    continue;
                }

            } catch (IOException | InterruptedException ex) {
                // Log full exception info because ex.getMessage() can be null (e.g. NPE, some IOExceptions)
                logger.warn("Gemini({}) call exception → retry {}/{}: {}",
                        correlationId, attempt, maxRetries, ex.toString());
            }

            Thread.sleep(backoff);
            backoff = Math.min(backoff * 2, 30000); // max 30s
        }
        throw new IOException("Gemini failed after " + maxRetries + " retries");
    }


    /* Helpers ----------------------------------------------------------- */

    private boolean isSuccess(int code) {
        return code >= 200 && code < 300;
    }

    private boolean isRetryable(int code) {
        return code == 429 || code == 500 || code == 503;
    }


    /* Google Payload ----------------------------------------------------- */

    private ObjectNode buildGooglePayload(String prompt) {
        ObjectNode root = mapper.createObjectNode();

        ArrayNode contents = root.putArray("contents");
        ObjectNode msg = mapper.createObjectNode();

        msg.put("role", "user");
        ArrayNode parts = msg.putArray("parts");

        ObjectNode text = mapper.createObjectNode();
        text.put("text", prompt);

        parts.add(text);
        contents.add(msg);

        ObjectNode config = mapper.createObjectNode();
        config.put("responseMimeType", "application/json");

        ObjectNode thinking = mapper.createObjectNode();
        thinking.put("thinkingBudget", 0);
        config.set("thinkingConfig", thinking);

        root.set("generationConfig", config);

        return root;
    }


    /* Google JSON extract ------------------------------------------------ */

    private String extractGoogleText(String body) throws IOException {
        JsonNode root = mapper.readTree(body);

        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray()) return body;

        for (JsonNode candidate : candidates) {
            JsonNode content = candidate.path("content");

            if (content.has("parts")) {
                StringBuilder out = new StringBuilder();
                for (JsonNode p : content.path("parts")) {
                    String t = p.path("text").asText(null);
                    if (t != null && !t.isBlank()) out.append(t);
                }
                if (out.length() > 0) return out.toString();
            }

            if (content.has("text")) {
                String t = content.path("text").asText("");
                if (!t.isBlank()) return t;
            }
        }

        return body;
    }

    /* Mock provider ------------------------------------------------ */
    private String buildMockPayload(String prompt) throws JsonProcessingException {
        // Đếm số lượng bài trong prompt để trả về đúng số lượng mock item
        int itemCount = 1;
        try {
            // Tìm chuỗi "Input items: " trong prompt để parse JSON
            int idx = prompt.indexOf("Input items: ");
            if (idx >= 0) {
                String jsonPart = prompt.substring(idx + 13).trim();
                // Cắt bớt phần thừa nếu có (ví dụ "\nReturn ONLY...")
                int endIdx = jsonPart.indexOf("\nReturn ONLY");
                if (endIdx > 0) jsonPart = jsonPart.substring(0, endIdx);
                
                JsonNode items = mapper.readTree(jsonPart);
                if (items.isArray()) {
                    itemCount = items.size();
                }
            }
        } catch (Exception e) {
            logger.warn("Mock parse failed, defaulting to 1 item: {}", e.getMessage());
        }

        // Build mock response dynamic theo số lượng item
        ArrayNode summaries = mapper.createArrayNode();
        for (int i = 0; i < itemCount; i++) {
            ObjectNode s = mapper.createObjectNode();
            s.put("title", "Mock Summary Title " + (i + 1));
            s.put("url", "http://example.com/mock-" + (i + 1));
            
            ArrayNode bullets = s.putArray("bullets");
            bullets.add("Đây là tóm tắt giả lập bài số " + (i + 1));
            bullets.add("Hệ thống đang chạy chế độ Mock.");
            bullets.add("Google API đang bị rate limit.");
            
            s.put("why_it_matters", "Giúp kiểm thử luồng hoạt động mà không cần gọi API thật.");
            s.put("type", "news");
            summaries.add(s);
        }
        
        ObjectNode root = mapper.createObjectNode();
        root.set("summaries", summaries);
        
        return mapper.writeValueAsString(root);
    }

    private void incrementExternalCalls() {
        if (externalCallCounter != null) {
            externalCallCounter.increment();
        }
    }
}
