package com.example.summarizer.ports;

import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.domain.SummaryResult;

import java.util.List;
import java.util.Map;

public interface SummarizeUseCase {
    List<SummaryResult> summarize(List<FeedArticle> articles) throws Exception;
    List<SummaryResult> summarize(List<FeedArticle> articles, Map<String, SummaryResult> cache) throws Exception;
}
