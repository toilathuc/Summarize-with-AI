package com.example.summarizer.tools;

import com.example.summarizer.pipelines.NewsPipeline;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * Replacement for the Python Celery task run_pipeline_task.
 * Provides a method to run the news pipeline with an optional correlation id.
 */
@Service
public class PipelineTaskService {

    private final NewsPipeline pipeline;

    public PipelineTaskService(NewsPipeline pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * Runs the pipeline and returns the resulting path as a string.
     * This method restores the correlation id into MDC so logs include it.
     */
    public String runPipelineTask(String jobId, String correlationId, int top) throws Exception {
        if (correlationId != null && !correlationId.isBlank()) {
            MDC.put("correlationId", correlationId);
            // Also set a JVM system property (best-effort) so subprocesses can read it if needed
            System.setProperty("X_CORRELATION_ID", correlationId);
        }

        Path out = pipeline.run(top);

        // Clear MDC after run
        if (correlationId != null && !correlationId.isBlank()) {
            MDC.remove("correlationId");
        }

        return out == null ? "" : out.toAbsolutePath().toString();
    }
}
