package com.example.summarizer.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
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
    private static final String JINA_PREFIX = "https://r.jina.ai/";

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
        this.formats = (formats == null || formats.isEmpty()) ? List.of("markdown") : List.copyOf(formats);
        this.parsers = parsers == null ? List.of() : List.copyOf(parsers);
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
        Optional<String> fc = fetchFromFirecrawl(targetUrl);
        if (fc.isPresent()) return fc;

        Optional<String> jina = fetchFromJina(targetUrl);
        if (jina.isPresent()) return jina;

        return fetchFromJsoup(targetUrl);
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
        formats.forEach(f::add);

        ArrayNode p = root.putArray("parsers");
        parsers.forEach(p::add);

        return root;
    }

    private Optional<String> fetchFromJina(String targetUrl) {
        logger.info("Fallback to Jina AI for: {}", targetUrl);
        try {
            String jinaUrl = JINA_PREFIX + targetUrl;

            HttpRequest request = HttpRequest.newBuilder(URI.create(jinaUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) {
                String body = resp.body();
                try {
                    JsonNode root = mapper.readTree(body);
                    if (root.has("data") && root.get("data").has("content")) {
                        return Optional.of(root.get("data").get("content").asText());
                    }
                } catch (Exception ignored) {
                }

                if (body != null && body.length() > 100) {
                    logger.info("Jina AI success ({} chars)", body.length());
                    return Optional.of(body);
                }
            }
            logger.warn("Jina AI failed with status: {}", resp.statusCode());
        } catch (Exception e) {
            logger.warn("Jina AI exception: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<String> fetchFromJsoup(String targetUrl) {
        logger.info("Fallback to Jsoup (Local) for: {}", targetUrl);
        try {
            Document doc = Jsoup.connect(targetUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .timeout(30000)
                    .get();

            String title = doc.title();
            String body = doc.body().text();

            if (body.length() > 200) {
                String markdown = "# " + title + "\n\n" + body;
                logger.info("Jsoup success ({} chars)", markdown.length());
                return Optional.of(markdown);
            }
        } catch (Exception e) {
            logger.warn("Jsoup failed: {}", e.getMessage());
        }
        return Optional.empty();
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
