package com.example.summarizer.ports;

import java.nio.file.Path;

public interface RefreshNewsUseCase {
    Path run(int top) throws Exception;
}
