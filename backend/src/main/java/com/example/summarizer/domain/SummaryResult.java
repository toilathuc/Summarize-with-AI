package com.example.summarizer.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SummaryResult {
    private String title;
    private String url;

    private List<String> bullets = new ArrayList<>();

    @JsonProperty("why_it_matters")
    private String whyItMatters;

    private String type;

    public SummaryResult() {}

    public SummaryResult(String title, String url, List<String> bullets, String whyItMatters, String type) {
        this.title = title;
        this.url = url;
        this.bullets = bullets;
        this.whyItMatters = whyItMatters;
        this.type = type;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public List<String> getBullets() { return bullets; }
    public void setBullets(List<String> bullets) { this.bullets = bullets; }

    public String getWhyItMatters() { return whyItMatters; }
    public void setWhyItMatters(String whyItMatters) { this.whyItMatters = whyItMatters; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public static SummaryResult fromJson(JsonNode node) {
        String title = node.path("title").asText(null);
        String url = node.path("url").asText(null);
        List<String> bullets = new ArrayList<>();
        if (node.has("bullets") && node.path("bullets").isArray()) {
            for (JsonNode b : node.path("bullets")) bullets.add(b.asText());
        }
        String why = node.path("why_it_matters").asText(null);
        String type = node.path("type").asText(null);
        return new SummaryResult(title, url, bullets, why, type);
    }
}
