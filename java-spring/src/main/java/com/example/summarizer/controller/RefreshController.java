package com.example.summarizer.controller;

import com.example.summarizer.tools.PipelineTaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
public class RefreshController {

    private final PipelineTaskService pipelineTaskService;

    public RefreshController(PipelineTaskService pipelineTaskService) {
        this.pipelineTaskService = pipelineTaskService;
    }

    @PostMapping("/api/refresh")
    public ResponseEntity<?> triggerRefresh(
            @RequestParam(name = "top", defaultValue = "20") int top,
            @RequestHeader(name = "X-Correlation-ID", required = false) String incomingCorrelationId
    ) {
        String jobId = UUID.randomUUID().toString();
        String correlationId = incomingCorrelationId != null && !incomingCorrelationId.isBlank()
                ? incomingCorrelationId
                : jobId;

        try {
            String path = pipelineTaskService.runPipelineTask(jobId, correlationId, top);
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "job_id", jobId,
                    "result_path", path,
                    "top", top,
                    "correlation_id", correlationId
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "job_id", jobId,
                    "message", ex.getMessage()
            ));
        }
    }
}
