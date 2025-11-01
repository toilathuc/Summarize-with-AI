package com.example.summarizer.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
            // Build Google Generative API request shape for /v1beta2/models/{model}:generate
            // Body example: { "prompt": { "text": "..." }, "temperature": 0.0 }
            ObjectNode root = mapper.createObjectNode();
            ObjectNode promptNode = mapper.createObjectNode();
            promptNode.put("text", prompt);
            root.set("prompt", promptNode);
            root.put("temperature", 0.0);
            payload = mapper.writeValueAsString(root);
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
        while (true) {
            try {
                HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int code = resp.statusCode();
                if (code >= 200 && code < 300) {
                    String body = resp.body();
                    // If provider=google, extract textual output from JSON response
                    if ("google".equalsIgnoreCase(provider)) {
                        try {
                            JsonNode root = mapper.readTree(body);
                            // v1 responses may have "candidates" or "output" or "candidates" inside "candidates"
                            if (root.has("candidates") && root.path("candidates").isArray() && root.path("candidates").size() > 0) {
                                JsonNode first = root.path("candidates").get(0);
                                if (first.has("content")) return first.path("content").asText();
                                if (first.has("output")) return first.path("output").asText();
                            }
                            // Some variants: "output" may be an array of objects with "content"
                            if (root.has("output") && root.path("output").isArray() && root.path("output").size() > 0) {
                                JsonNode f = root.path("output").get(0);
                                if (f.has("content")) return f.path("content").asText();
                            }
                        } catch (Exception ex) {
                            // Fallback to raw body
                            return body;
                        }
                        return body;
                    }
                    return body;
                } else {
                    attempt++;
                    if (attempt > maxRetries) {
                        throw new IOException("Non-2xx response from Gemini endpoint: " + code + ": " + resp.body());
                    }
                }
            } catch (IOException | InterruptedException ex) {
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
