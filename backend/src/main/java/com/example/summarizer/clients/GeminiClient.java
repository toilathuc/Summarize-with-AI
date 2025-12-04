package com.example.summarizer.clients;

import com.example.summarizer.ports.SummarizerPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

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

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    public GeminiClient(String apiKey, String model, int maxRetries, String endpointUrl,
                        String provider, boolean useApiKeyAsQuery) {

        this.apiKey = apiKey;
        this.model = model;
        this.maxRetries = Math.max(1, maxRetries);
        this.endpoint = URI.create(endpointUrl);
        this.provider = provider == null ? "raw" : provider;
        this.useApiKeyAsQuery = useApiKeyAsQuery;

        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }


    @Override
    public String generate(String prompt) throws IOException, InterruptedException {

        String payload =
                "google".equalsIgnoreCase(provider)
                        ? mapper.writeValueAsString(buildGooglePayload(prompt))
                        : mapper.writeValueAsString(new RequestBody(model, prompt));

        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                                + "Chrome/125.0 Safari/537.36");

        if ("google".equalsIgnoreCase(provider) && useApiKeyAsQuery) {
            String sep = endpoint.getQuery() == null ? "?" : "&";
            rb.uri(URI.create(endpoint + sep + "key=" + apiKey));
        } else {
            rb.uri(endpoint);
            rb.header("Authorization", "Bearer " + apiKey);
        }

        HttpRequest request = rb.POST(HttpRequest.BodyPublishers.ofString(payload)).build();

        // correlation id theo request → debug dễ hơn
        String correlationId = UUID.randomUUID().toString().substring(0, 8);

        logger.debug("Gemini({}) → Prepared request: provider={}, model={}, endpoint={}",
                correlationId, provider, model, endpoint);

        long backoff = 500;
        int attempt = 0;

        while (true) {
            attempt++;

            try {
                logger.debug("Gemini({}) attempt {}", correlationId, attempt);

                HttpResponse<String> resp =
                        http.send(request, HttpResponse.BodyHandlers.ofString());

                int code = resp.statusCode();
                String body = resp.body();

                if (isSuccess(code)) {
                    logger.debug("Gemini({}) success ({} chars)", correlationId,
                            body == null ? 0 : body.length());

                    if ("google".equalsIgnoreCase(provider)) {
                        try {
                            return extractGoogleText(body);
                        } catch (Exception ex) {
                            logger.warn("Gemini({}) parse-google failed → using raw", correlationId);
                        }
                    }
                    return body;
                }

                if (!isRetryable(code)) {
                    logger.error("Gemini({}) non-retryable {}: {}", correlationId, code, body);
                    throw new IOException("Gemini error " + code + ": " + body);
                }

                logger.warn("Gemini({}) retryable {} → retry {} of {}", correlationId, code, attempt, maxRetries);

            } catch (IOException | InterruptedException ex) {
                if (attempt > maxRetries) {
                    logger.error("Gemini({}) FAILED after {} retries: {}", correlationId, maxRetries, ex.getMessage());
                    throw ex;
                }

                logger.warn("Gemini({}) call exception → retry {}/{}: {}",
                        correlationId, attempt, maxRetries, ex.getMessage());
            }

            Thread.sleep(backoff);
            backoff = Math.min(backoff * 2, 5000); // max 5s
        }
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


    private static class RequestBody {
        public final String model;
        public final String prompt;
        public RequestBody(String model, String prompt) {
            this.model = model;
            this.prompt = prompt;
        }
    }
}
