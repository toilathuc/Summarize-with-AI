package com.example.summarizer.feeds;

import com.example.summarizer.domain.FeedArticle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
public class TechmemeFeedClient {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<FeedArticle> fetchArticles(Integer limit) throws IOException {
        Path file = Path.of("data", "raw", "techmeme_sample_full.json");
        JsonNode root = mapper.readTree(file.toFile());
        List<FeedArticle> out = new ArrayList<>();
        Iterator<JsonNode> it = root.elements();
        int count = 0;
        while (it.hasNext()) {
            if (limit != null && count >= limit) break;
            JsonNode n = it.next();
            String title = n.path("title").asText("");
            String url = n.path("original_url").asText(null);
            if (url == null || url.isBlank()) url = n.path("techmeme_url").asText(null);
            String content = n.path("summary_text").asText(null);
            FeedArticle a = new FeedArticle(title, url, content);
            out.add(a);
            count++;
        }
        return out;
    }
}
