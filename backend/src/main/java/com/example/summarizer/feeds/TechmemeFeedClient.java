package com.example.summarizer.feeds;

import com.example.summarizer.domain.FeedArticle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

@Component
public class TechmemeFeedClient {

    private static final Logger logger = LoggerFactory.getLogger(TechmemeFeedClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String feedUrl;

    private static final int MAX_RETRY = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(600);

    public TechmemeFeedClient(
            RestTemplateBuilder builder,
            @Value("${feeds.techmeme.url:https://www.techmeme.com/feed.xml}") String feedUrl
    ) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();

        this.feedUrl = feedUrl;
    }

    // -----------------------------------------------------------------------
    // PUBLIC API
    // -----------------------------------------------------------------------
    public List<FeedArticle> fetchArticles(Integer limit) throws IOException {

        List<FeedArticle> rss = fetchRssWithRetry(limit);

        if (!rss.isEmpty()) {
            logger.debug("Fetched {} RSS articles from Techmeme", rss.size());
            return rss;
        }

        logger.warn("⚠️ Techmeme RSS failed. Falling back to sample JSON file...");
        List<FeedArticle> fallback = readFromSample(limit);

        if (fallback.isEmpty()) {
            logger.error("❌ Fallback sample file empty – returning empty list!");
        }

        return fallback;
    }

    // -----------------------------------------------------------------------
    // RETRY LOGIC
    // -----------------------------------------------------------------------
    private List<FeedArticle> fetchRssWithRetry(Integer limit) {
        for (int i = 1; i <= MAX_RETRY; i++) {
            try {
                List<FeedArticle> items = fetchFromRss(limit);
                if (!items.isEmpty()) return items;

                logger.warn("RSS attempt {} failed — empty or invalid. Retrying...", i);

            } catch (Exception ex) {
                logger.warn("RSS attempt {} failed: {}", i, ex.getMessage());
            }

            try {
                Thread.sleep(RETRY_DELAY.toMillis());
            } catch (InterruptedException ignored) {}
        }
        return List.of();
    }

    // -----------------------------------------------------------------------
    // RSS FETCH
    // -----------------------------------------------------------------------
    private List<FeedArticle> fetchFromRss(Integer limit) {
        try {
            String xml = restTemplate.getForObject(feedUrl, String.class);

            if (xml == null || xml.isBlank()) {
                logger.warn("RSS returned empty body");
                return List.of();
            }

            return parseRss(xml, limit);

        } catch (RestClientException | IOException ex) {
            logger.error("Failed to fetch Techmeme RSS: {}", ex.getMessage());
            logger.debug("Techmeme RSS exception", ex);
            return List.of();
        }
    }

    // -----------------------------------------------------------------------
    // RSS PARSING
    // -----------------------------------------------------------------------
    private List<FeedArticle> parseRss(String xml, Integer limit) throws IOException {
        Document doc = buildDocument(xml);

        NodeList items = doc.getElementsByTagName("item");
        List<FeedArticle> out = new ArrayList<>();

        for (int i = 0; i < items.getLength(); i++) {
            if (limit != null && out.size() >= limit) break;

            Node node = items.item(i);
            if (!(node instanceof Element element)) continue;

            String title = textOf(element, "title");
            String rawDesc = textOf(element, "description");
            String fallbackLink = textOf(element, "link");

            String external = extractExternalLink(rawDesc);
            String finalLink = external != null ? external : fallbackLink;

            if (finalLink == null || finalLink.isBlank()) continue;

            String cleanDesc = sanitizeDescription(rawDesc);

            FeedArticle article = new FeedArticle(
                    title != null ? title : "",
                    finalLink.trim(),
                    cleanDesc == null ? "" : cleanDesc
            );

            out.add(article);
        }

        logger.debug("Parsed {} items from Techmeme RSS", out.size());
        return out;
    }

    private Document buildDocument(String xml) throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            // disable XXE
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (ParserConfigurationException ignored) {}

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xml)));

        } catch (ParserConfigurationException | SAXException ex) {
            throw new IOException("Failed to parse RSS XML", ex);
        }
    }

    private String textOf(Element element, String tag) {
        NodeList nodes = element.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return null;
        return nodes.item(0).getTextContent();
    }

    // -----------------------------------------------------------------------
    // DESCRIPTION / LINK EXTRACTION
    // -----------------------------------------------------------------------
    private String sanitizeDescription(String raw) {
        if (raw == null) return null;
        String noTags = raw.replaceAll("(?s)<[^>]+>", " ");
        return noTags.replaceAll("\\s+", " ").trim();
    }

    private String extractExternalLink(String descriptionHtml) {
        if (descriptionHtml == null || descriptionHtml.isBlank()) return null;

        try {
            org.jsoup.nodes.Document doc = Jsoup.parseBodyFragment(descriptionHtml);
            Elements anchors = doc.select("a[href]");

            for (org.jsoup.nodes.Element a : anchors) {
                String href = a.attr("href").trim();
                if (href.isBlank()) continue;
                if (isTechmemeLink(href)) continue;
                return href;
            }

        } catch (Exception ex) {
            logger.debug("Failed to extract external link: {}", ex.getMessage());
        }

        return null;
    }

    private boolean isTechmemeLink(String href) {
        String lower = href.toLowerCase(Locale.ROOT);
        return lower.contains("techmeme.com");
    }

    // -----------------------------------------------------------------------
    // FALLBACK SAMPLE
    // -----------------------------------------------------------------------
    private List<FeedArticle> readFromSample(Integer limit) throws IOException {
        Path file = Path.of("data", "raw", "techmeme_sample_full.json");

        JsonNode root = mapper.readTree(file.toFile());
        List<FeedArticle> out = new ArrayList<>();

        int count = 0;
        for (JsonNode n : root) {
            if (limit != null && count >= limit) break;

            String title = n.path("title").asText("");
            String url = n.path("original_url").asText("");
            if (url.isBlank()) url = n.path("techmeme_url").asText("");
            if (url.isBlank()) continue;

            String description = n.path("summary_text").asText("");

            out.add(new FeedArticle(title, url, description));
            count++;
        }

        logger.debug("Loaded {} fallback articles", out.size());
        return out;
    }
}
