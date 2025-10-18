"""
Legacy compatibility layer for Techmeme feed helpers.

The heavy lifting now lives inside `src.feeds.techmeme.client`. This module
keeps the historical function-based API (make_session, fetch_feed, etc.) used by
existing scripts and tests.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Optional

import feedparser
import requests

from src.config import settings
from src.feeds.techmeme.client import (
    TechmemeFeedClient,
    as_plain_dict,
    enrich_from_article,
    normalize_feed,
)

ROOT_DIR = Path(__file__).resolve().parents[2]
DEFAULT_OUTPUT_PATH = ROOT_DIR / "data" / "raw" / "techmeme_sample_full.json"


def make_session(timeout: int = 12) -> requests.Session:
    """Create a configured requests session."""

    client = TechmemeFeedClient(timeout=timeout)
    return client.session


def fetch_feed(session: requests.Session, url: Optional[str] = None) -> feedparser.FeedParserDict:
    """Fetch the Techmeme feed using a pre-configured session."""

    client = TechmemeFeedClient(feed_url=url, session=session, timeout=getattr(session, "request_timeout", 12))
    return client.fetch_raw_feed()


def main() -> None:
   

    parser = argparse.ArgumentParser(description="Fetch Techmeme RSS and store it as JSON (optional enrichment).")
    parser.add_argument("--feed", default=settings.feed.url, help="RSS URL (default: Techmeme)")
    parser.add_argument("--enrich", type=int, default=5, help="Enrich top N items (0 = skip)")
    parser.add_argument("--out", default=str(DEFAULT_OUTPUT_PATH), help="Output JSON path")
    parser.add_argument("--timeout", type=int, default=settings.feed.timeout, help="HTTP timeout seconds")
    args = parser.parse_args()

    client = TechmemeFeedClient(feed_url=args.feed, timeout=args.timeout)

    try:
        feed = client.fetch_raw_feed()
        items = normalize_feed(feed)

        enrich_count = max(0, int(args.enrich))
        for item in items[:enrich_count]:
            url = item.get("original_url") or item.get("techmeme_url")
            try:
                item["meta"] = enrich_from_article(client.session, url)
            except requests.RequestException:
                item["meta"] = {}

        print(f"Total items: {len(items)}")
        for entry in items[:5]:
            print("-" * 60)
            print("TITLE        :", entry.get("title"))
            print("TECHMEME_URL :", entry.get("techmeme_url"))
            print("ORIGINAL_URL :", entry.get("original_url"))
            if entry.get("related_urls"):
                print("RELATED_URLS :", entry["related_urls"][:3])
            print("PUBLISHED_AT :", entry.get("published_at"))
            print("AUTHOR       :", entry.get("author_name"))
            print("HAS_CONTENT_HTML:", bool(entry.get("content_html")))
            if entry.get("meta"):
                meta = entry["meta"]
                print("OG_TITLE     :", meta.get("og_title"))
                print("OG_IMAGE     :", meta.get("og_image"))

        payload = items[:20]
        with open(args.out, "w", encoding="utf-8") as handle:
            json.dump(payload, handle, ensure_ascii=False, indent=2)
        print(f"Wrote output to: {args.out}")

    except requests.RequestException as exc:
        print(f"Network or SSL error: {exc}")


if __name__ == "__main__":
    main()

