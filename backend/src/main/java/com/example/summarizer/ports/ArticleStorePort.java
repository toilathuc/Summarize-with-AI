package com.example.summarizer.ports;

import com.example.summarizer.domain.FeedArticle;

import java.io.IOException;
import java.util.List;

public interface ArticleStorePort {
    List<FeedArticle> fetchLatestFromTable(Integer limit) throws IOException;
    void replaceAll(List<FeedArticle> articles, String source) throws IOException;
    boolean isEmpty() throws IOException;
}
