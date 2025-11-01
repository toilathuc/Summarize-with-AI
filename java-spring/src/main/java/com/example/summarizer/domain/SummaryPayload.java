package com.example.summarizer.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SummaryPayload {

    @JsonProperty("items")
    private List<SummaryResult> summaries;

    private final Map<String, Object> extra = new HashMap<>();

    public SummaryPayload() {}

    public SummaryPayload(List<SummaryResult> summaries) {
        this.summaries = summaries;
    }

    public List<SummaryResult> getSummaries() { return summaries; }
    public void setSummaries(List<SummaryResult> summaries) { this.summaries = summaries; }

    public void putExtra(String key, Object value) { this.extra.put(key, value); }

    @JsonAnyGetter
    public Map<String, Object> any() { return extra; }
}
