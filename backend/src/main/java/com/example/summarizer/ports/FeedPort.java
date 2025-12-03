package com.example.summarizer.ports;

import com.example.summarizer.domain.FeedArticle;

import java.io.IOException;
import java.util.List;

public interface FeedPort {
    List<FeedArticle> fetchLatest(Integer limit) throws IOException;
}
