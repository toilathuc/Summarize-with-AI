package com.example.summarizer.config;

import com.example.summarizer.ports.ClockPort;
import com.example.summarizer.ports.LoadSummariesQuery;
import com.example.summarizer.ports.SummaryStorePort;
import com.example.summarizer.service.SummarizationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public LoadSummariesQuery loadSummariesQuery(SummaryStorePort summaryStore, ClockPort clock) {
        return new SummarizationService(summaryStore, clock);
    }
}
