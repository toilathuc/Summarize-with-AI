package com.example.summarizer.domain;

import java.util.Map;

public class SummaryRequest {
    private String title;
    private String url;
    private String content;

    public SummaryRequest() {}

    public SummaryRequest(String title, String url, String content) {
        this.title = title;
        this.url = url;
        this.content = content;
    }

    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getContent() { return content; }

    public Map<String, Object> toPromptMap() {
        return Map.of(
                "title", title,
                "url", url,
                "content", content
        );
    }
}
