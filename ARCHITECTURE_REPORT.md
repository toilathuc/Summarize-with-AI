# Architecture Report

## Current Stack

- Backend: Spring Boot 3
- LLM: Groq
- Crawl: Firecrawl only
- Cache: Redis
- Persistence: SQLite
- Feed source: Techmeme RSS

## Pipeline

1. Fetch Techmeme RSS
2. Extract the source URL from each item
3. Send source URL to Firecrawl
4. Build summary prompt from fetched content
5. Call Groq and store the result

## Notes

- Older crawl fallback layers were removed.
- The LLM provider was replaced by Groq.
- If Firecrawl fails or quota is exhausted, the application falls back to the RSS description or existing content already present on the article object.

## Main Components

- `FeedClient`
- `CrawlClient`
- `ContentCrawlerService`
- `GroqClient`
- `SummarizationOrchestrator`
- `RefreshCoordinator`

## Build Status

The backend source compiles with the current Firecrawl-only and Groq-based setup.
