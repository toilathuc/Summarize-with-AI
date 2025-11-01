package com.example.summarizer.controller;

import com.example.summarizer.service.SummarizationService;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
public class SummariesController {

    private final SummarizationService service;

    public SummariesController(SummarizationService service) {
        this.service = service;
    }

    @GetMapping("/api/summaries")
    public ResponseEntity<?> getSummaries() {
        try {
            Map<String, Object> data = service.getSummaries();
            // Attach correlation id from MDC
            data.put("correlation_id", MDC.get("correlationId"));
            return ResponseEntity.ok(data);
        } catch (IOException ex) {
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/healthz")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
