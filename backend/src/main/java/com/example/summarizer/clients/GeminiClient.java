package com.example.summarizer.clients;

import com.example.summarizer.ports.SummarizerPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

public class GeminiClient implements SummarizerPort {

    private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final URI endpoint;
    private final int maxRetries;
    private final String provider;
    private final boolean useApiKeyAsQuery;
    private final Counter externalCallCounter;

    public GeminiClient(
            String apiKey,
            String model,
            int maxRetries,
            String endpointUrl,
            String provider,
            boolean useApiKeyAsQuery,
            MeterRegistry registry
    ) {
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
        if ("mock".equalsIgnoreCase(provider)) {
            logger.info("Gemini mock mode: returning fake summary");
            return buildMockPayload(prompt);
        }

        String payload = mapper.writeValueAsString(buildGooglePayload(prompt));

        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                                + "Chrome/125.0 Safari/537.36");

        if (useApiKeyAsQuery) {
            String sep = endpoint.getQuery() == null ? "?" : "&";
            rb.uri(URI.create(endpoint + sep + "key=" + apiKey));
        } else {
            rb.uri(endpoint);
            rb.header("Authorization", "Bearer " + apiKey);
        }

        HttpRequest request = rb.POST(HttpRequest.BodyPublishers.ofString(payload)).build();
        String correlationId = UUID.randomUUID().toString().substring(0, 8);

        logger.info("Gemini({}) prepared request: provider={}, model={}, endpoint={}",
                correlationId, provider, model, endpoint);

        long backoff = 5000;
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

                HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
                int code = resp.statusCode();
                String body = resp.body();

                if (isSuccess(code)) {
                    logger.info("Gemini({}) success ({} chars)", correlationId, body == null ? 0 : body.length());
                    try {
                        return extractGoogleText(body);
                    } catch (Exception ex) {
                        logger.warn("Gemini({}) Google parse failed, using raw body", correlationId);
                        return body;
                    }
                }

                if (!isRetryable(code)) {
                    logger.error("Gemini({}) non-retryable {}: {}", correlationId, code, body);
                    throw new IOException("Gemini error " + code + ": " + body);
                }

                logger.warn("Gemini({}) retryable {} on attempt {}/{}", correlationId, code, attempt, maxRetries);

                if (code == 429) {
                    logger.warn("Gemini({}) hit 429, sleeping 60s", correlationId);
                    Thread.sleep(60000);
                    backoff = 5000;
                    continue;
                }
            } catch (IOException | InterruptedException ex) {
                logger.warn("Gemini({}) call exception on retry {}/{}: {}", correlationId, attempt, maxRetries, ex);
            }

            Thread.sleep(backoff);
            backoff = Math.min(backoff * 2, 30000);
        }

        throw new IOException("Gemini failed after " + maxRetries + " retries");
    }

    private boolean isSuccess(int code) {
        return code >= 200 && code < 300;
    }

    private boolean isRetryable(int code) {
        return code == 429 || code == 500 || code == 503;
    }

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

    private String buildMockPayload(String prompt) throws JsonProcessingException {
        int itemCount = 1;
        try {
            int idx = prompt.indexOf("Input items: ");
            if (idx >= 0) {
                String jsonPart = prompt.substring(idx + 13).trim();
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

        ArrayNode summaries = mapper.createArrayNode();
        for (int i = 0; i < itemCount; i++) {
            ObjectNode s = mapper.createObjectNode();
            s.put("title", "Mock Summary Title " + (i + 1));
            s.put("url", "http://example.com/mock-" + (i + 1));

            ArrayNode bullets = s.putArray("bullets");
            bullets.add("This is a fake summary for item " + (i + 1));
            bullets.add("The system is running in mock mode.");
            bullets.add("The Google API is rate limited.");

            s.put("why_it_matters", "This helps test the workflow without calling the real API.");
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
