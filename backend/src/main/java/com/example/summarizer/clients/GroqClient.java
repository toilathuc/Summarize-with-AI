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

public class GroqClient implements SummarizerPort {

    private static final Logger logger = LoggerFactory.getLogger(GroqClient.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private static final String DEFAULT_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";

    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final URI endpoint;
    private final int maxRetries;
    private final String mode;
    private final Counter externalCallCounter;

    public GroqClient(
            String apiKey,
            String model,
            int maxRetries,
            String endpointUrl,
            String mode,
            MeterRegistry registry
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "llama-3.1-8b-instant" : model.trim();
        this.maxRetries = Math.max(1, maxRetries);
        this.endpoint = URI.create(resolveEndpoint(endpointUrl));
        this.mode = mode == null || mode.isBlank() ? "live" : mode.trim();
        this.externalCallCounter = registry == null ? null
                : registry.counter("summarizer_external_calls_total", "service", "groq");

        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String generate(String prompt) throws IOException, InterruptedException {
        if ("mock".equalsIgnoreCase(mode)) {
            logger.info("Groq mock mode: returning fake summary");
            return buildMockPayload(prompt);
        }

        if (apiKey.isBlank()) {
            throw new IOException("Groq API key is missing");
        }

        String payload = mapper.writeValueAsString(buildChatPayload(prompt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(endpoint)
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                                + "Chrome/125.0 Safari/537.36")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        String correlationId = UUID.randomUUID().toString().substring(0, 8);

        logger.info("Groq({}) prepared request: mode={}, model={}, endpoint={}",
                correlationId, mode, model, endpoint);

        long backoff = 3000;
        int attempts = maxRetries + 1;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                logger.info("Groq({}) attempt {}/{}", correlationId, attempt, attempts);
                incrementExternalCalls();

                HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
                int code = resp.statusCode();
                String body = resp.body();

                if (isSuccess(code)) {
                    logger.info("Groq({}) success ({} chars)", correlationId, body == null ? 0 : body.length());
                    try {
                        return extractGroqText(body);
                    } catch (Exception ex) {
                        logger.warn("Groq({}) response parse failed, using raw body", correlationId);
                        return body;
                    }
                }

                if (!isRetryable(code) || attempt == attempts) {
                    logger.error("Groq({}) non-retryable {}: {}", correlationId, code, body);
                    throw new IOException("Groq error " + code + ": " + body);
                }

                logger.warn("Groq({}) retryable {} on attempt {}/{}", correlationId, code, attempt, attempts);
                Thread.sleep(backoff);
                backoff = Math.min(backoff * 2, 30000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw ex;
            } catch (IOException ex) {
                if (attempt == attempts) {
                    throw ex;
                }
                logger.warn("Groq({}) call exception on retry {}/{}: {}", correlationId, attempt, attempts, ex.getMessage());
                Thread.sleep(backoff);
                backoff = Math.min(backoff * 2, 30000);
            }
        }

        throw new IOException("Groq failed after " + maxRetries + " retries");
    }

    private String resolveEndpoint(String endpointUrl) {
        if (endpointUrl == null || endpointUrl.isBlank() || endpointUrl.contains("example.com")) {
            return DEFAULT_ENDPOINT;
        }
        return endpointUrl.trim();
    }

    private boolean isSuccess(int code) {
        return code >= 200 && code < 300;
    }

    private boolean isRetryable(int code) {
        return code == 429 || code == 500 || code == 503;
    }

    private ObjectNode buildChatPayload(String prompt) {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.2);

        ArrayNode messages = root.putArray("messages");

        ObjectNode system = mapper.createObjectNode();
        system.put("role", "system");
        system.put("content", "Return only valid JSON for the user's request.");
        messages.add(system);

        ObjectNode user = mapper.createObjectNode();
        user.put("role", "user");
        user.put("content", prompt);
        messages.add(user);

        return root;
    }

    private String extractGroqText(String body) throws IOException {
        JsonNode root = mapper.readTree(body);
        JsonNode choices = root.path("choices");
        if (!choices.isArray()) {
            return body;
        }

        for (JsonNode choice : choices) {
            JsonNode message = choice.path("message");
            JsonNode content = message.path("content");

            if (content.isTextual()) {
                String text = content.asText("");
                if (!text.isBlank()) {
                    return text;
                }
            }

            if (content.isArray()) {
                StringBuilder out = new StringBuilder();
                for (JsonNode part : content) {
                    String text = part.path("text").asText(null);
                    if (text != null && !text.isBlank()) {
                        out.append(text);
                    }
                }
                if (out.length() > 0) {
                    return out.toString();
                }
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
                if (endIdx > 0) {
                    jsonPart = jsonPart.substring(0, endIdx);
                }

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
            bullets.add("Groq API was not called.");

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
