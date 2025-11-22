package com.example.summarizer.ports;

import com.example.summarizer.domain.SummaryPayload;
import com.example.summarizer.domain.SummaryResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface SummaryStorePort {
    Path save(List<SummaryResult> summaries, Map<String, Object> extra) throws IOException;
    SummaryPayload loadExisting() throws IOException;
}
