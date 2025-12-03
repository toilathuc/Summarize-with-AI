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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.io.StringReader;

@Component
public class TechmemeFeedClient {
    private static final Logger logger = LoggerFactory.getLogger(TechmemeFeedClient.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate restTemplate;
    private final String feedUrl;

    public TechmemeFeedClient(RestTemplateBuilder builder,
                              @Value("${feeds.techmeme.url:https://www.techmeme.com/feed.xml}") String feedUrl) {
        this.restTemplate = builder.setConnectTimeout(java.time.Duration.ofSeconds(10))
                .setReadTimeout(java.time.Duration.ofSeconds(10))
                .build();
        this.feedUrl = feedUrl;
    }

    public List<FeedArticle> fetchArticles(Integer limit) throws IOException {
        List<FeedArticle> items = fetchFromRss(limit);
        if (!items.isEmpty()) {
            logger.debug("Fetched {} articles from Techmeme RSS", items.size());
            return items;
        }
        logger.warn("Techmeme RSS fetch failed or returned empty. Falling back to sample JSON file.");
        return readFromSample(limit);
    }

    private List<FeedArticle> fetchFromRss(Integer limit) {
        try {
            String xml = restTemplate.getForObject(feedUrl, String.class);
            if (xml == null || xml.isBlank()) return List.of();
            return parseRss(xml, limit);
        } catch (RestClientException | IOException ex) {
            logger.error("Failed to fetch Techmeme RSS feed: {}", ex.getMessage());
            logger.debug("Techmeme RSS exception", ex);
            return List.of();
        }
    }

    private List<FeedArticle> parseRss(String xml, Integer limit) throws IOException {
        Document doc = buildDocument(xml);
        NodeList items = doc.getElementsByTagName("item");
        List<FeedArticle> out = new ArrayList<>();
        for (int i = 0; i < items.getLength(); i++) {
            if (limit != null && out.size() >= limit) break;
            Node node = items.item(i);
            if (!(node instanceof Element element)) continue;
            String title = textOf(element, "title");
            String descriptionRaw = textOf(element, "description");
            String link = selectArticleLink(descriptionRaw, textOf(element, "link"));
            String desc = sanitizeDescription(descriptionRaw);
            if ((title == null || title.isBlank()) && (link == null || link.isBlank())) {
                continue;
            }
            String content = desc == null || desc.isBlank() ? "" : desc.trim();
            FeedArticle article = new FeedArticle(title, link, content);
            out.add(article);
        }
        logger.debug("Parsed {} RSS entries from Techmeme", out.size());
        return out;
    }

    private Document buildDocument(String xml) throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (ParserConfigurationException ignored) {}
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource source = new InputSource(new StringReader(xml));
            return builder.parse(source);
        } catch (ParserConfigurationException | SAXException ex) {
            throw new IOException("Failed to parse RSS feed", ex);
        }
    }

    private String textOf(Element element, String tag) {
        NodeList nodes = element.getElementsByTagName(tag);
        if (nodes == null || nodes.getLength() == 0) return null;
        Node node = nodes.item(0);
        return node != null ? node.getTextContent() : null;
    }

    private String sanitizeDescription(String raw) {
        if (raw == null) return null;
        String withoutTags = raw.replaceAll("(?s)<[^>]+>", " ");
        return withoutTags.replaceAll("\s+", " ").trim();
    }

    private String selectArticleLink(String descriptionHtml, String fallbackLink) {
        String preferred = extractExternalLink(descriptionHtml);
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallbackLink;
    }

    private String extractExternalLink(String descriptionHtml) {
        if (descriptionHtml == null || descriptionHtml.isBlank()) {
            return null;
        }
        try {
            org.jsoup.nodes.Document fragment = Jsoup.parseBodyFragment(descriptionHtml);
            Elements anchors = fragment.select("a[href]");
            for (org.jsoup.nodes.Element anchor : anchors) {
                String href = anchor.attr("href").trim();
                if (href.isBlank()) {
                    continue;
                }
                if (isTechmemeLink(href)) {
                    continue;
                }
                return href;
            }
        } catch (Exception ex) {
            logger.debug("Failed to extract external link from Techmeme description: {}", ex.getMessage());
            logger.trace("Techmeme description parsing exception", ex);
        }
        return null;
    }

    private boolean isTechmemeLink(String href) {
        String lower = href.toLowerCase(Locale.ROOT);
        return lower.contains("techmeme.com");
    }

    private List<FeedArticle> readFromSample(Integer limit) throws IOException {
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
        logger.debug("Loaded {} articles from techmeme_sample_full.json", out.size());
        return out;
    }
}