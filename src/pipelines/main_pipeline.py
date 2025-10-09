"""
Backward-compatible entry point that delegates to the modular NewsPipeline.
"""

from __future__ import annotations

from pathlib import Path

from src.pipelines.news_pipeline import NewsPipeline
from src.services.storage_service import StorageService

# Maintain legacy alias expected by some scripts/tests.
NewsProcessor = NewsPipeline


def run(top_n: int = 20) -> None:
    """Fetch feed, summarise the top items, and persist them for the frontend."""

    pipeline = NewsPipeline()
    payload = pipeline.run(top_n=top_n)
    output_path = StorageService().output_path

    print(f"Saved {len(payload.summaries)} summaries to {output_path.relative_to(Path.cwd())}")


if __name__ == "__main__":
    run()
