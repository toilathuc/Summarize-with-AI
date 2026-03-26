package com.example.summarizer.tools;

import com.example.summarizer.ports.RefreshNewsUseCase;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.nio.file.Path;


@Service
public class PipelineTaskService {

    private final RefreshNewsUseCase pipeline;

    public PipelineTaskService(RefreshNewsUseCase pipeline) {
        this.pipeline = pipeline;
    }

    
    public String runPipelineTask(String jobId, String correlationId, int top) throws Exception {
        if (correlationId != null && !correlationId.isBlank()) {
            MDC.put("correlationId", correlationId);
            
            System.setProperty("X_CORRELATION_ID", correlationId);
        }

        Path out = pipeline.run(top);

        
        if (correlationId != null && !correlationId.isBlank()) {
            MDC.remove("correlationId");
        }

        return out == null ? "" : out.toAbsolutePath().toString();
    }
}
