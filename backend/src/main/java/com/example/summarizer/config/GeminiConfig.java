package com.example.summarizer.config;

import com.example.summarizer.clients.GeminiClient;
import com.example.summarizer.ports.CachePort;
import com.example.summarizer.ports.SummarizeUseCase;
import com.example.summarizer.ports.SummarizerPort;
import com.example.summarizer.service.SummarizationOrchestrator;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Value("${gemini.apiKey:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${gemini.model:gemini-1}")
    private String model;

    @Value("${gemini.endpoint:}")
    private String endpoint;

    @Value("${gemini.provider:google}")
    private String provider;

    @Value("${gemini.maxRetries:2}")
    private int maxRetries;

    @Value("${gemini.useApiKeyAsQuery:true}")
    private boolean useApiKeyAsQuery;

    @Value("${summarizer.batchSize:8}")
    private int batchSize;

    @Value("${summarizer.promptTemplate:{items_json}}")
    private String promptTemplate;

    @Bean
    public SummarizerPort geminiClient(MeterRegistry registry) {
        String effectiveEndpoint = resolveEndpoint();
        String safeKey = apiKey != null ? apiKey.trim() : "";

        return new GeminiClient(
                safeKey,
                model,
                maxRetries,
                effectiveEndpoint,
                provider,
                useApiKeyAsQuery,
                registry
        );
    }

    private String resolveEndpoint() {
        String target = endpoint;
        if (target == null || target.isBlank() || target.contains("example.com")) {
            target = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent";
        }

        if (target.contains("{model}")) {
            return target.replace("{model}", model);
        }

        return target;
    }

    @Bean
    public SummarizeUseCase summarizationOrchestrator(SummarizerPort client, CachePort cache) {
        String safeTemplate = promptTemplate;
        if (safeTemplate == null || safeTemplate.isBlank()) {
            safeTemplate = "{items_json}";
        }

        return new SummarizationOrchestrator(client, cache, safeTemplate, batchSize);
    }
}
