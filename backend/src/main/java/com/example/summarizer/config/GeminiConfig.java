package com.example.summarizer.config;

import com.example.summarizer.clients.GeminiClient;
import com.example.summarizer.ports.SummarizeUseCase;
import com.example.summarizer.ports.SummarizerPort;
import com.example.summarizer.service.SummarizationOrchestrator;

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

    @Value("${gemini.provider:raw}")
    private String provider;

    @Value("${gemini.maxRetries:2}")
    private int maxRetries;

    @Value("${gemini.useApiKeyAsQuery:true}")
    private boolean useApiKeyAsQuery;

    @Value("${summarizer.batchSize:8}")
    private int batchSize;

    @Value("${summarizer.promptTemplate:{items_json}}")
    private String promptTemplate;


    /**
     * Build GeminiClient bean
     */
    @Bean
    public SummarizerPort geminiClient() {

        String effectiveEndpoint = resolveEndpoint();

        return new GeminiClient(
                apiKey,
                model,
                maxRetries,
                effectiveEndpoint,
                provider,
                useApiKeyAsQuery
        );
    }


    /**
     * Resolve đúng endpoint theo provider
     */
    private String resolveEndpoint() {

        // RAW Provider → dùng endpoint người dùng config
        if (!"google".equalsIgnoreCase(provider)) {
            return endpoint == null || endpoint.isBlank()
                    ? "https://api.example.com/v1/generate"
                    : endpoint;
        }

        // GOOGLE Provider
        if (endpoint == null || endpoint.isBlank() || endpoint.contains("example.com")) {
            // Default Google Gemini endpoint
            return "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent";
        }

        // Người dùng tự custom endpoint
        if (endpoint.contains("{model}")) {
            return endpoint.replace("{model}", model);
        }

        return endpoint;
    }


    /**
     * Build SummarizationOrchestrator
     */
    @Bean
    public SummarizeUseCase summarizationOrchestrator(SummarizerPort client) {
        String safeTemplate = promptTemplate;

        // đảm bảo template không bị rỗng
        if (safeTemplate == null || safeTemplate.isBlank()) {
            safeTemplate = "{items_json}";
        }

        return new SummarizationOrchestrator(client, safeTemplate, batchSize);
    }
}
