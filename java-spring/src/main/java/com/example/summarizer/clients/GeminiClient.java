package com.example.summarizer.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Minimal REST-based Gemini client with simple retry logic.
 * Endpoint URL is configurable; body is JSON: {"model":..., "prompt":...}
 */
public class GeminiClient {

    private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final int maxRetries;
    private final URI endpoint;
    private final String provider; // "raw" or "google"
    private final boolean useApiKeyAsQuery;

    public GeminiClient(String apiKey, String model, int maxRetries, String endpointUrl) {
        this(apiKey, model, maxRetries, endpointUrl, "raw", false);
    }

    public GeminiClient(String apiKey, String model, int maxRetries, String endpointUrl, String provider) {
        this(apiKey, model, maxRetries, endpointUrl, provider, false);
    }

    public GeminiClient(String apiKey, String model, int maxRetries, String endpointUrl, String provider, boolean useApiKeyAsQuery) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxRetries = Math.max(0, maxRetries);
        this.endpoint = URI.create(endpointUrl);
        this.provider = provider == null ? "raw" : provider;
        this.useApiKeyAsQuery = useApiKeyAsQuery;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Generate text for the given prompt. Returns the textual output (not raw JSON) when possible.
     */
    public String generate(String prompt) throws IOException, InterruptedException {
        String payload;
        if ("google".equalsIgnoreCase(provider)) {
            payload = mapper.writeValueAsString(buildGooglePayload(prompt));
        } else {
            // Default raw shape: {"model":..., "prompt":...}
            payload = mapper.writeValueAsString(new RequestBody(model, prompt));
        }

        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .uri(endpoint)
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json");

        // For Google provider we may use API key as query parameter instead of Authorization header
        if ("google".equalsIgnoreCase(provider) && useApiKeyAsQuery && apiKey != null && !apiKey.isBlank()) {
            // Append ?key=... or &key=... depending on existing query
            String sep = endpoint.getQuery() == null ? "?" : "&";
            URI newUri = URI.create(endpoint.toString() + sep + "key=" + apiKey);
            rb.uri(newUri);
        } else if (apiKey != null && !apiKey.isBlank()) {
            rb.header("Authorization", "Bearer " + apiKey);
        }

        HttpRequest request = rb.POST(HttpRequest.BodyPublishers.ofString(payload)).build();

        int attempt = 0;
        long backoffMs = 500;
        logger.debug("Gemini request prepared (provider={}, model={}, endpoint={})", provider, model, endpoint);
        while (true) {
            try {
                logger.debug("Gemini call attempt {}", attempt + 1);
                HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int code = resp.statusCode();
                if (code >= 200 && code < 300) {
                    String body = resp.body();
                    logger.debug("Gemini response OK ({} chars)", body == null ? 0 : body.length());
                    if ("google".equalsIgnoreCase(provider)) {
                        try {
                            return extractGoogleText(body);
                        } catch (Exception ex) {
                            logger.warn("Failed to parse Google Gemini payload, returning raw body: {}", ex.getMessage());
                        }
                    }
                    return body;
                } else {
                    logger.warn("Gemini responded with status {} on attempt {}", code, attempt + 1);
                    attempt++;
                    if (attempt > maxRetries) {
                        throw new IOException("Non-2xx response from Gemini endpoint: " + code + ": " + resp.body());
                    }
                }
            } catch (IOException | InterruptedException ex) {
                logger.warn("Gemini call failed on attempt {}: {}", attempt + 1, ex.getMessage());
                attempt++;
                if (attempt > maxRetries) {
                    throw ex;
                }
            }

            try {
                Thread.sleep(backoffMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw ie;
            }
            backoffMs = Math.min(backoffMs * 2, 5000);
        }
    }

    private ObjectNode buildGooglePayload(String prompt) {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode contents = root.putArray("contents");
        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        ArrayNode parts = userMessage.putArray("parts");
        ObjectNode textPart = mapper.createObjectNode();
        textPart.put("text", prompt);
        parts.add(textPart);
        contents.add(userMessage);

        ObjectNode generationConfig = mapper.createObjectNode();
        generationConfig.put("responseMimeType", "application/json");
        ObjectNode thinkingConfig = mapper.createObjectNode();
        thinkingConfig.put("thinkingBudget", 0);
        generationConfig.set("thinkingConfig", thinkingConfig);
        root.set("generationConfig", generationConfig);
        return root;
    }

    private String extractGoogleText(String body) throws IOException {
        JsonNode root = mapper.readTree(body);
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray()) {
            for (JsonNode candidate : candidates) {
                JsonNode content = candidate.path("content");
                if (content.has("parts") && content.path("parts").isArray()) {
                    StringBuilder builder = new StringBuilder();
                    for (JsonNode part : content.path("parts")) {
                        String text = part.path("text").asText(null);
                        if (text != null) builder.append(text);
                    }
                    if (builder.length() > 0) {
                        return builder.toString();
                    }
                }
                if (content.has("text")) {
                    String text = content.path("text").asText(null);
                    if (text != null && !text.isBlank()) {
                        return text;
                    }
                }
            }
        }
        return body;
    }

    // Simple helper used to build request JSON for raw provider
    private static class RequestBody {
        public final String model;
        public final String prompt;

        public RequestBody(String model, String prompt) {
            this.model = model;
            this.prompt = prompt;
        }
    }
}
