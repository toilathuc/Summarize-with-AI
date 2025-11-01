package com.example.summarizer.config;

import com.example.summarizer.clients.GeminiClient;
import com.example.summarizer.service.SummarizationOrchestrator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Value("${gemini.apiKey:}")
    private String apiKey;

    @Value("${gemini.model:gemini-1}")
    private String model;

    @Value("${gemini.endpoint:https://api.example.com/v1/generate}")
    private String endpoint;

    @Value("${gemini.provider:raw}")
    private String provider;

    @Value("${gemini.maxRetries:2}")
    private int maxRetries;

    @Value("${summarizer.batchSize:8}")
    private int batchSize;

    @Value("${summarizer.promptTemplate:{items_json}}")
    private String promptTemplate;

    @Bean
    public GeminiClient geminiClient() {
        // If provider is google and the endpoint is still the placeholder, set the Google Generative API endpoint
        String effectiveEndpoint = endpoint;
        if ("google".equalsIgnoreCase(provider)) {
            if (endpoint == null || endpoint.contains("example.com") || endpoint.trim().isEmpty()) {
                effectiveEndpoint = "https://generativelanguage.googleapis.com/v1beta2/models/" + model + ":generate";
            } else if (endpoint.contains("{model}")) {
                effectiveEndpoint = endpoint.replace("{model}", model);
            }
        }

        return new GeminiClient(apiKey, model, maxRetries, effectiveEndpoint, provider, useApiKeyAsQuery);
    }

    @Value("${gemini.useApiKeyAsQuery:true}")
    private boolean useApiKeyAsQuery;

    @Bean
    public SummarizationOrchestrator summarizationOrchestrator(GeminiClient client) {
        return new SummarizationOrchestrator(client, promptTemplate, batchSize);
    }
}
