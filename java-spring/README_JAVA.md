Java Spring Boot skeleton for Summarize-with-AI

This folder contains a minimal Spring Boot (Maven) skeleton targeting JDK 21.

What is included:

- pom.xml with Spring Boot starter web
- Basic controller exposing `/api/summaries` (reads `data/outputs/summaries.json`)
- Correlation ID servlet filter that reads `X-Correlation-ID` or generates one and injects into logs

How to build (Windows PowerShell):

```powershell
cd java-spring
mvn -v
mvn -DskipTests package
java -jar target/summarizer-0.0.1-SNAPSHOT.jar
```

Notes:

- The service reads the JSON file at `data/outputs/summaries.json` relative to the workspace root. Keep the same path as the Python project.
- Gemini client and other services are not yet ported; this is an incremental scaffold.
- Feed ingestion now uses the Techmeme RSS feed (`feeds.techmeme.url`, default `https://www.techmeme.com/feed.xml`). The articles are cached inside the SQLite DB (`storage.database-path`) so repeated runs don't hit the network unless the cache is empty. If the network fetch fails, the service falls back to the bundled `data/raw/techmeme_sample_full.json` file.
- The UI “refresh” button now POSTs to `/api/refresh`, which runs `NewsPipeline` (fetch RSS → summarize → persist) and, once complete, reloads the summaries via `/api/summaries`. You can also hit that endpoint manually for a full refresh.
- RSS descriptions are parsed to grab the first non-Techmeme link (e.g., the Bloomberg URL in the sample feed), so cards open the publisher article while still falling back to the Techmeme permalink when no external link is available.
- When you set `gemini.provider=google`, the client now uses the official `v1beta/models/{model}:generateContent` endpoint with the `contents` payload shape (no proxy required). Just add `gemini.apiKey` and the model name (`gemini-flash-latest`, `gemini-pro`, etc.) and the pipeline will call Google directly.
- `summarizer.promptTemplate` now defaults to an instruction that forces JSON output (`{"summaries": [...]}`). Only tweak it if you know the downstream parser requirements.

## Firecrawl-powered article crawling

- Set `firecrawl.enabled=true` plus `firecrawl.apiKey=fc-...` **or** export `FIRECRAWL_API_KEY` to let the pipeline enrich each RSS item with full-page markdown pulled from Firecrawl Cloud.
- Defaults such as `firecrawl.endpoint`, `firecrawl.onlyMainContent`, `firecrawl.maxAge`, and the comma-delimited `firecrawl.formats`/`firecrawl.parsers` can be overridden inside `application.properties` when you need different scrape formats.
- Use `firecrawl.minContentLength` (default `100`) to automatically re-crawl cached articles whose stored snippet is too short, and flip `firecrawl.alwaysRefresh=true` if you want to refresh every cached entry regardless of length.
- When Firecrawl is disabled or fails, the pipeline falls back to the short RSS description, so summaries still run albeit with limited context.

## Gemini direct calls

- Set `gemini.provider=google` (default) plus `gemini.model=gemini-flash-latest` (or any public model) and the client will hit `v1beta/models/{model}:generateContent` directly.
- Provide credentials via `gemini.apiKey=...` or export `GEMINI_API_KEY` to avoid checking secrets into source control.
- The Summarization orchestrator expects the model to emit the JSON array described in `summarizer.promptTemplate`; avoid adding Markdown fences or conversational text around the payload.
- Summaries are cached by URL/title: rerunning the pipeline reuses existing bullets when nothing changed, and only the new/updated articles trigger Gemini calls.
