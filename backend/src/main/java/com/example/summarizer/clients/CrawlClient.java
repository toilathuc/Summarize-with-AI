package com.example.summarizer.clients;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

public class CrawlClient {

    private static final Logger logger = LoggerFactory.getLogger(CrawlClient.class);
    private static final int MAX_RETRY = 3;
    private static final long INITIAL_BACKOFF_MS = 350;
    private static final long DOMAIN_COOLDOWN_MS = 60_000;
    private static final Pattern BAD_URL = Pattern.compile("^#|javascript:|^\\s*$");
    private static final String QUOTA_EXPIRED_MESSAGE = "Firecrawl quota exhausted";

    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final URI endpoint;
    private final String apiKey;
    private final boolean enabled;
    private final boolean onlyMainContent;
    private final Long maxAge;
    private final Duration timeout;
    private final Counter externalCallCounter;
    private final Semaphore throttle = new Semaphore(3);

    private static final Map<String, Long> DOMAIN_COOLDOWN = new HashMap<>();
    private static final Set<String> PAYWALL_DOMAINS = Set.of(
            "wsj.com", "ft.com", "bloomberg.com", "economist.com"
    );
    private static final List<String> USER_AGENTS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/126.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/124.0 Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0) AppleWebKit/605.1 Safari/604.1"
    );

    public CrawlClient(
            String endpointUrl,
            String apiKey,
            boolean enabled,
            boolean onlyMainContent,
            Long maxAge,
            List<String> formats,
            List<String> parsers,
            Duration timeout,
            MeterRegistry registry
    ) {
        this.endpoint = endpointUrl == null ? null : URI.create(endpointUrl);
        this.apiKey = apiKey;
        this.enabled = enabled && apiKey != null && !apiKey.isBlank() && endpoint != null;
        this.onlyMainContent = onlyMainContent;
        this.maxAge = maxAge != null && maxAge > 0 ? maxAge : null;
        this.timeout = timeout == null ? Duration.ofSeconds(45) : timeout;
        this.externalCallCounter = registry == null ? null
                : registry.counter("summarizer_external_calls_total", "service", "firecrawl");

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();

        if (enabled && !this.enabled) {
            logger.warn("Firecrawl disabled: missing API key or endpoint");
        }
    }

    public Optional<String> fetchMarkdown(String targetUrl) {
        return fetchFromFirecrawl(targetUrl);
    }

    private Optional<String> fetchFromFirecrawl(String targetUrl) {
        if (!enabled || !validateUrl(targetUrl)) return Optional.empty();
        incrementExternalCalls();

        if (isPaywallDomain(targetUrl)) {
            logger.warn("Skipping paywall domain: {}", targetUrl);
            return Optional.empty();
        }

        String domain = extractDomain(targetUrl);
        long now = System.currentTimeMillis();
        if (domain != null) {
            Long last = DOMAIN_COOLDOWN.get(domain);
            if (last != null && now - last < DOMAIN_COOLDOWN_MS) {
                logger.warn("Skip domain [{}] due to cooldown ({} ms left)", domain, DOMAIN_COOLDOWN_MS - (now - last));
                return Optional.empty();
            }
        }

        logger.debug("Firecrawl request: {}", targetUrl);

        try {
            throttle.acquire();
            Thread.sleep(120 + new Random().nextInt(120));

            String payload = mapper.writeValueAsString(buildPayload(targetUrl));
            String ua = USER_AGENTS.get(new Random().nextInt(USER_AGENTS.size()));

            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", ua)
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            Optional<String> result = executeWithRetry(request, targetUrl);
            if (domain != null) {
                DOMAIN_COOLDOWN.put(domain, System.currentTimeMillis());
            }
            return result;
        } catch (Exception ex) {
            logger.warn("Firecrawl failed for {}: {}", targetUrl, ex.getMessage());
            return Optional.empty();
        } finally {
            throttle.release();
        }
    }

    private Optional<String> executeWithRetry(HttpRequest request, String url)
            throws IOException, InterruptedException {
        long backoff = INITIAL_BACKOFF_MS;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            logger.info("Firecrawl attempt {}/{}: {}", attempt, MAX_RETRY, url);

            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();

            if (code >= 200 && code < 300) {
                return parseMarkdown(resp.body()).map(md -> {
                    logger.info("Firecrawl success ({} chars): {}", md.length(), url);
                    return md;
                });
            }

            if (code == 402) {
                logger.warn("{} for {}", QUOTA_EXPIRED_MESSAGE, url);
                logErrorDetail(resp.body());
                return Optional.empty();
            }

            if (isQuotaExpiredBody(resp.body())) {
                logger.warn("{} for {}", QUOTA_EXPIRED_MESSAGE, url);
                logErrorDetail(resp.body());
                return Optional.empty();
            }

            if (code == 429 || code == 503) {
                logger.warn("Rate limit {} for {} (attempt {}/{})", code, url, attempt, MAX_RETRY);

                Optional<Long> retryAfter = parseRetryAfter(resp);
                if (retryAfter.isPresent()) {
                    Thread.sleep(retryAfter.get());
                } else if (attempt < MAX_RETRY) {
                    Thread.sleep(backoff);
                    backoff = Math.min(backoff * 2, 3000);
                } else {
                    logger.warn("Max retries reached for {}, giving up", url);
                }
                continue;
            }

            logger.warn("Firecrawl returned {} for {}", code, url);
            logErrorDetail(resp.body());
            return Optional.empty();
        }

        return Optional.empty();
    }

    private Optional<Long> parseRetryAfter(HttpResponse<String> resp) {
        try {
            String header = resp.headers().firstValue("Retry-After").orElse(null);
            if (header != null) {
                return Optional.of(Long.parseLong(header) * 1000);
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    private void logErrorDetail(String body) {
        try {
            JsonNode root = mapper.readTree(body);
            JsonNode err = root.path("error");
            if (!err.isMissingNode()) {
                logger.warn("Firecrawl error detail: {}", err.toString());
            }
        } catch (Exception ignored) {
        }
    }

    public boolean isQuotaExpiredBody(String body) {
        if (body == null || body.isBlank()) {
            return false;
        }

        String lower = body.toLowerCase();
        return lower.contains("quota")
                || lower.contains("limit exceeded")
                || lower.contains("billing")
                || lower.contains("payment required")
                || lower.contains("subscription");
    }

    private boolean validateUrl(String url) {
        return url != null && !BAD_URL.matcher(url).find();
    }

    private boolean isPaywallDomain(String url) {
        return PAYWALL_DOMAINS.stream().anyMatch(url::contains);
    }

    private Optional<String> parseMarkdown(String body) throws IOException {
        JsonNode root = mapper.readTree(body);
        if (!root.path("success").asBoolean(false)) return Optional.empty();

        String text = root.path("data").path("markdown").asText(null);
        return (text == null || text.isBlank()) ? Optional.empty() : Optional.of(text.trim());
    }

    private String extractDomain(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private ObjectNode buildPayload(String url) {
        ObjectNode root = mapper.createObjectNode();
        root.put("url", url);
        root.put("onlyMainContent", onlyMainContent);

        if (maxAge != null) root.put("maxAge", maxAge);
        ArrayNode f = root.putArray("formats");
        f.add("markdown");

        return root;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private void incrementExternalCalls() {
        if (externalCallCounter != null) {
            externalCallCounter.increment();
        }
    }
}
