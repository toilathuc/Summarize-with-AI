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
