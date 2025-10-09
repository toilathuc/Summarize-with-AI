# Techmeme RSS Summarizer

This project collects the latest items from the Techmeme RSS feed, optionally enriches them, and generates concise AI summaries for downstream consumption.

## Project layout

```
Demo_Skola/
+- .env                       # Gemini API key and other secrets
+- README.md                  # Project documentation (this file)
+- public/
¦  +- index.html              # Simple frontend stub that can render summaries.json
+- data/
¦  +- raw/                    # Source data snapshots
¦  ¦  +- techmeme_sample_full.json
¦  +- outputs/                # Generated artifacts for the UI or other consumers
¦     +- summaries.json
+- src/
   +- __init__.py
   +- feeds/
   ¦  +- __init__.py
   ¦  +- techmeme.py          # RSS fetcher + optional article enrichment
   +- pipelines/
   ¦  +- __init__.py
   ¦  +- main_pipeline.py     # Entry point that ties fetching and summarising together
   +- services/
      +- __init__.py
      +- summarize_with_gemini.py  # Gemini wrapper used by the pipeline
```

## Prerequisites

- Python 3.10+
- Dependencies: `requests`, `feedparser`, `beautifulsoup4`, `google-generativeai`, `python-dotenv`
- A valid Gemini API key stored in `.env` as `GEMINI_API_KEY`

## Usage

1. **Install dependencies**

   ```powershell
   pip install -r requirements.txt
   ```

   (If you do not maintain a requirements file yet, install the libraries listed above manually.)

2. **Populate the `.env` file**

   ```env
   GEMINI_API_KEY=your_real_key_here
   ```

3. **Fetch and inspect the raw feed (optional)**

   ```powershell
   python -m src.feeds.techmeme --out data/raw/techmeme_sample_full.json
   ```

   This produces a raw snapshot under `data/raw/` so you can explore the structure without invoking Gemini.

4. **Generate AI summaries**

   ```powershell
   python -m src.pipelines.main_pipeline
   ```

   The pipeline fetches the latest items, keeps the top 20 entries, and writes the summaries to `data/outputs/summaries.json`.

## Notes

- Always run the commands from the project root so the package imports resolve correctly.
- `src/services/summarize_with_gemini.py` handles Gemini retries and fallback behaviour; tweak the prompt or batching logic there.
- The raw and output folders are git-tracked for convenience. If you prefer to keep generated data out of version control, update your `.gitignore` accordingly.
- The `public/index.html` file can be served by any static host; it expects `data/outputs/summaries.json` to be available.
