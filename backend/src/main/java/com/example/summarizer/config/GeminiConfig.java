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

    @Value("${gemini.model:gemini-1.5-flash-latest}")
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
     * Resolve Google Gemini endpoint
     */
    private String resolveEndpoint() {

        // Nếu không phải Google provider → dùng endpoint người dùng ghi
        if (!"google".equalsIgnoreCase(provider)) {
            return (endpoint == null || endpoint.isBlank())
                    ? "https://api.example.com/v1/generate"
                    : endpoint;
        }

        // Nếu user đã tự nhập endpoint → dùng luôn
        if (endpoint != null && !endpoint.isBlank() && !endpoint.contains("example.com")) {
            return endpoint.trim();
        }

        // Google provider → auto generate endpoint đúng chuẩn
        return "https://generativelanguage.googleapis.com/v1/models/"
                + model.trim()
                + ":generateContent";
    }


    /**
     * Build Orchestrator
     */
    @Bean
    public SummarizeUseCase summarizationOrchestrator(SummarizerPort client) {

        String safeTemplate = (promptTemplate == null || promptTemplate.isBlank())
                ? "{items_json}"
                : promptTemplate;

        return new SummarizationOrchestrator(client, safeTemplate, batchSize);
    }
}
