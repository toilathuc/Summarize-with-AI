package com.example.summarizer.domain;

public class FeedArticle {
    private String title;
    private String url;
    private String description;
    private String content;
    private Boolean isSummarized = Boolean.FALSE;

    public FeedArticle() {}

    public FeedArticle(String title, String url, String description) {
        this.title = title;
        this.url = url;
        this.description = description;
    }

    public FeedArticle(String title, String url, String description, String content, Boolean isSummarized) {
        this.title = title;
        this.url = url;
        this.description = description;
        this.content = content;
        this.isSummarized = isSummarized;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Boolean getIsSummarized() { return isSummarized; }
    public void setIsSummarized(Boolean isSummarized) { this.isSummarized = isSummarized; }

    public SummaryRequest toSummaryRequest() {
        return new SummaryRequest(title, url, content);
    }
}
