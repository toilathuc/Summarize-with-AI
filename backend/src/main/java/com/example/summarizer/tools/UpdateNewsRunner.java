package com.example.summarizer.tools;

import com.example.summarizer.service.RefreshCoordinator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

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

        String cid = "cli-" + System.currentTimeMillis();

        // ⭐ Gọi async nhưng đợi kết quả như CLI đúng nghĩa
        CompletableFuture<Path> future = coordinator.runAsyncRefresh(top, cid);

        // ⭐ Đợi async hoàn thành (block)
        Path out = future.get();

        System.out.println("Update complete. Wrote: " + out.toAbsolutePath());

        // Mimic CLI behaviour: exit ngay
        System.exit(0);
    }
}
