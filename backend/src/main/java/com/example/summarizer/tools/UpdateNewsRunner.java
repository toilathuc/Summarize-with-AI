package com.example.summarizer.tools;

import com.example.summarizer.service.RefreshCoordinator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class UpdateNewsRunner implements CommandLineRunner {

    private final RefreshCoordinator coordinator;

    @Value("${update.now:false}")
    private boolean updateNow;

    @Value("${update.top:25}")
    private int top;

    public UpdateNewsRunner(RefreshCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!updateNow) return;
        System.out.println("Running news update pipeline (top=" + top + ")...");
        String correlationId = "cli-" + System.currentTimeMillis();
        java.nio.file.Path out = coordinator.runAsyncRefresh(top, correlationId);
        System.out.println("Update complete. Wrote: " + out.toAbsolutePath().toString());
        // Exit after run to mimic CLI behaviour
        System.exit(0);
    }
}
