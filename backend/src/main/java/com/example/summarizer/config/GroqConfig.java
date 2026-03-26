package com.example.summarizer.config;

import com.example.summarizer.clients.GroqClient;
import com.example.summarizer.ports.CachePort;
import com.example.summarizer.ports.SummarizeUseCase;
import com.example.summarizer.ports.SummarizerPort;
import com.example.summarizer.service.SummarizationOrchestrator;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GroqConfig {

    @Value("${groq.apiKey:${GROQ_API_KEY:}}")
    private String apiKey;

    @Value("${groq.model:llama-3.1-8b-instant}")
    private String model;

    @Value("${groq.endpoint:}")
    private String endpoint;

    @Value("${groq.mode:live}")
    private String mode;

    @Value("${groq.maxRetries:2}")
    private int maxRetries;

    @Value("${summarizer.batchSize:8}")
    private int batchSize;

    @Value("${summarizer.promptTemplate:{items_json}}")
    private String promptTemplate;

    @Bean
    public SummarizerPort groqClient(MeterRegistry registry) {
        String safeKey = apiKey != null ? apiKey.trim() : "";

        return new GroqClient(
                safeKey,
                model,
                maxRetries,
                endpoint,
                mode,
                registry
        );
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
