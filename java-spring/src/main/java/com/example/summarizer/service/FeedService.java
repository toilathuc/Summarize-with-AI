package com.example.summarizer.service;

import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.feeds.TechmemeFeedClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class FeedService {

    private final TechmemeFeedClient client;

    public FeedService(TechmemeFeedClient client) {
        this.client = client;
    }

    public List<FeedArticle> fetchLatest(Integer limit) throws IOException {
        return client.fetchArticles(limit);
    }
}
