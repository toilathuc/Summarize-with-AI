package com.example.summarizer.config;

import com.example.summarizer.clients.CrawlClient;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class FirecrawlConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirecrawlConfig.class);

    @Value("${firecrawl.endpoint:https://api.firecrawl.dev/v2/scrape}")
    private String endpoint;

    @Value("${firecrawl.apiKey:${FIRECRAWL_API_KEY:}}")
    private String apiKey;

    @Value("${firecrawl.enabled:false}")
    private boolean enabled;

    @Value("${firecrawl.onlyMainContent:true}")
    private boolean onlyMainContent;

    @Value("${firecrawl.maxAge:0}")
    private long maxAge;

    @Value("${firecrawl.formats:markdown}")
    private String formats;

    @Value("${firecrawl.parsers:}")
    private String parsers;

    @Value("${firecrawl.timeoutSeconds:45}")
    private int timeoutSeconds;

    @Bean
    public CrawlClient firecrawlClient(MeterRegistry registry) {

        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("Firecrawl disabled: missing API key");
            enabled = false;
        }

        Duration timeout = Duration.ofSeconds(Math.max(5, timeoutSeconds));
        Long maxAgeValue = maxAge > 0 ? maxAge : null;

        logger.info("""
                Firecrawl client configured:
                  - endpoint = {}
                  - enabled = {}
                  - onlyMain = {}
                  - maxAge = {}
                  - formats = {}
                  - parsers = {}
                  - timeout = {}s
                """,
                endpoint,
                enabled,
                onlyMainContent,
                maxAgeValue,
                parseList(formats),
                parseList(parsers),
                timeoutSeconds
        );

        return new CrawlClient(
                endpoint,
                apiKey,
                enabled,
                onlyMainContent,
                maxAgeValue,
                parseList(formats),
                parseList(parsers),
                timeout,
                registry
        );
    }

    private List<String> parseList(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }
}
