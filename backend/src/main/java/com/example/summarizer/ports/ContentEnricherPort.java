package com.example.summarizer.ports;

import com.example.summarizer.domain.FeedArticle;

import java.util.List;

public interface ContentEnricherPort {
    void enrich(List<FeedArticle> articles, boolean forceRefresh);
}
