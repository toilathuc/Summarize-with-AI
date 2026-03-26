package com.example.summarizer.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;


public class FirecrawlClient {

    private static final Logger logger = LoggerFactory.getLogger(FirecrawlClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final URI endpoint;
    private final String apiKey;
    private final boolean enabled;
    private final boolean onlyMainContent;
    private final Long maxAge;
    private final List<String> formats;
    private final List<String> parsers;
    private final Duration timeout;

    public FirecrawlClient(String endpointUrl,
                           String apiKey,
                           boolean enabled,
                           boolean onlyMainContent,
                           Long maxAge,
                           List<String> formats,
                           List<String> parsers,
                           Duration timeout) {
        this.endpoint = endpointUrl == null ? null : URI.create(endpointUrl);
        this.apiKey = apiKey;
        this.enabled = enabled && apiKey != null && !apiKey.isBlank() && endpoint != null;
        this.onlyMainContent = onlyMainContent;
        this.maxAge = maxAge != null && maxAge > 0 ? maxAge : null;
        this.formats = formats == null || formats.isEmpty() ? List.of("markdown") : List.copyOf(formats);
        this.parsers = parsers == null ? Collections.emptyList() : List.copyOf(parsers);
        this.timeout = timeout == null ? Duration.ofSeconds(45) : timeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        if (enabled && !this.enabled) {
            logger.warn("Firecrawl disabled: missing API key or endpoint");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    
    public Optional<String> fetchMarkdown(String targetUrl) {
        if (!enabled) return Optional.empty();
        if (targetUrl == null || targetUrl.isBlank()) return Optional.empty();
        try {
            logger.debug("Firecrawl scrape request => {}", targetUrl);
            String body = mapper.writeValueAsString(buildPayload(targetUrl));
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logger.warn("Firecrawl returned HTTP {} for {}", response.statusCode(), targetUrl);
                return Optional.empty();
            }
            Optional<String> markdown = parseMarkdown(response.body());
            markdown.ifPresent(md -> logger.debug("Firecrawl scrape success ({} chars) <= {}", md.length(), targetUrl));
            return markdown;
        } catch (IOException ex) {
            logger.warn("Failed to call Firecrawl for {}: {}", targetUrl, ex.getMessage());
            logger.debug("Firecrawl exception", ex);
            return Optional.empty();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private Optional<String> parseMarkdown(String responseBody) throws IOException {
        JsonNode root = mapper.readTree(responseBody);
        boolean success = root.path("success").asBoolean(false);
        if (!success) return Optional.empty();
        String markdown = root.path("data").path("markdown").asText(null);
        if (markdown == null || markdown.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(markdown.trim());
    }

    private ObjectNode buildPayload(String targetUrl) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("url", targetUrl);
        payload.put("onlyMainContent", onlyMainContent);
        if (maxAge != null) {
            payload.put("maxAge", maxAge);
        }
        ArrayNode formatsNode = payload.putArray("formats");
        formats.forEach(formatsNode::add);
        ArrayNode parserNode = payload.putArray("parsers");
        parsers.forEach(parserNode::add);
        return payload;
    }
}
