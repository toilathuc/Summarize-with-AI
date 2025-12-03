package com.example.summarizer.controller;

import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.domain.SummaryResult;
import com.example.summarizer.ports.SummarizeUseCase;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SummarizeController {

    private final SummarizeUseCase summarizer;

    public SummarizeController(SummarizeUseCase summarizer) {
        this.summarizer = summarizer;
    }

    @PostMapping("/api/summarize")
    public ResponseEntity<?> summarize(@RequestBody List<FeedArticle> articles) {
        try {
            List<SummaryResult> results = summarizer.summarize(articles);
            // Attach correlation id for tracing
            return ResponseEntity.ok().header("X-Correlation-ID", MDC.get("correlationId")).body(results);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", ex.getMessage()));
        }
    }
}
