package com.example.summarizer;

import com.example.summarizer.pipelines.NewsPipeline;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class InitPipelineRunner implements CommandLineRunner {

    private final NewsPipeline newsPipeline;

    public InitPipelineRunner(NewsPipeline newsPipeline) {
        this.newsPipeline = newsPipeline;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🚀 Running NewsPipeline on startup...");
        newsPipeline.run(20); // Tự động chạy pipeline, tạo summaries.json
        System.out.println("✅ NewsPipeline finished successfully.");
    }
}
