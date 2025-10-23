#!/usr/bin/env python3
"""Refresh Techmeme summaries and prepare data for the web UI."""

from __future__ import annotations

import argparse
import json
import logging
import os
import shutil
import sys
from pathlib import Path
from typing import Any

# Ensure the project root (containing `src/`) is importable when executing directly.
ROOT_DIR = Path(__file__).resolve().parent # Assuming update_news.py is in the project root
if str(ROOT_DIR) not in sys.path: # check if root dir is in sys.path 
    # If not, add it
    sys.path.insert(0, str(ROOT_DIR))

from src.pipelines.news_pipeline import NewsPipeline
from src.services.storage_service import StorageService

# Log_format is important for structured logging
# example detail:
# 
LOG_FORMAT = "%(asctime)s %(levelname)s [%(message)s]"
logging.basicConfig(level=logging.INFO, format=LOG_FORMAT)
logger = logging.getLogger("update_news")

# read correlation id from env if present
CORRELATION_ID = os.environ.get("X_CORRELATION_ID")
if CORRELATION_ID:
    logger = logging.LoggerAdapter(logger, {"correlation_id": CORRELATION_ID})

PUBLIC_SUMMARIES = ROOT_DIR / "summaries.json"


def run_pipeline(top_n: int) -> Path:
    """Execute the news pipeline and return the path of the generated payload."""
    pipeline = NewsPipeline()
    payload = pipeline.run(top_n=top_n)

    storage = StorageService()
    output_path = storage.output_path
    if not output_path.exists():
        # Defensive: ensure the payload exists even if a custom storage service was injected.
        storage.save(payload)
    return output_path


def copy_to_public_location(source: Path, destination: Path) -> Path:
    """Copy the generated summaries to the path served by the frontend."""
    if not source.exists():
        raise FileNotFoundError(f"Summaries file not found at {source}")
    shutil.copy2(source, destination)
    return destination


def format_summary(path: Path) -> str:
    """Produce a short textual summary of the refreshed dataset."""
    with path.open("r", encoding="utf-8") as handle:
        payload = json.load(handle)

    raw_items = payload.get("items", [])
    if isinstance(raw_items, list):
        items = raw_items
    elif isinstance(raw_items, tuple):
        items = list(raw_items)
    else:
        items = []
    total_items = len(items)
    last_updated = payload.get("last_updated", "unknown")
    first_title = "N/A"
    if items and isinstance(items[0], dict):
        first_title = str(items[0].get("title", "N/A"))

    lines = [
        "",
        "Refresh complete [OK]",
        f"Last updated : {last_updated}",
        f"Total items  : {total_items}",
    ]
    if total_items:
        lines.append(f"Top headline : {first_title}")
    return "\n".join(lines)


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Fetch the latest Techmeme news and summarise them.")
    parser.add_argument(
        "--top",
        type=int,
        default=25,
        help="Number of articles to fetch from the feed (default: 25).",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=PUBLIC_SUMMARIES,
        help="Public JSON location for the frontend (default: ./summaries.json).",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> None:
    args = parse_args(argv)

    if args.top <= 0:
        raise SystemExit("The --top value must be greater than zero.")

    print("Refreshing Techmeme summaries...")
    try:
        generated_path = run_pipeline(top_n=args.top)
        public_path = copy_to_public_location(generated_path, args.output)
    except Exception as exc:  # pylint: disable=broad-except
        raise SystemExit(f"Refresh failed: {exc}") from exc

    print(f"Pipeline output saved to: {generated_path}")
    if public_path != generated_path:
        print(f"Public copy available at: {public_path}")
    print(format_summary(public_path))


if __name__ == "__main__":
    main()
