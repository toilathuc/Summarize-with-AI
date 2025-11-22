package com.example.summarizer.ports;

import java.io.IOException;
import java.util.Map;

public interface LoadSummariesQuery {
    Map<String, Object> getSummaries() throws IOException;
}
