package com.example.summarizer.tools;

import com.example.summarizer.pipelines.NewsPipeline;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class UpdateNewsRunner implements CommandLineRunner {

    private final NewsPipeline pipeline;

    @Value("${update.now:false}")
    private boolean updateNow;

    @Value("${update.top:25}")
    private int top;

    public UpdateNewsRunner(NewsPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!updateNow) return;
        System.out.println("Running news update pipeline (top=" + top + ")...");
    java.nio.file.Path out = pipeline.run(top);
    System.out.println("Update complete. Wrote: " + out.toAbsolutePath().toString());
        // Exit after run to mimic CLI behaviour
        System.exit(0);
    }
}
