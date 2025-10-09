"""
Orchestrated pipeline that fetches, summarises, and persists Techmeme news.
"""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Dict, Optional

from src.domain import SummaryPayload
from src.services import FeedService, StorageService, SummarizationService


class NewsPipeline:
    """Composable pipeline for refreshing news summaries."""

    def __init__(
        self,
        *,
        feed_service: Optional[FeedService] = None,
        summarization_service: Optional[SummarizationService] = None,
        storage_service: Optional[StorageService] = None,
    ) -> None:
        self._feeds = feed_service or FeedService()
        self._summaries = summarization_service or SummarizationService()
        self._storage = storage_service or StorageService()

    def run(self, *, top_n: int = 20, metadata: Optional[Dict[str, str]] = None) -> SummaryPayload:
        articles = self._feeds.fetch_latest(limit=top_n)
        summaries = self._summaries.summarize(articles)

        payload = SummaryPayload(
            summaries=summaries,
            extra={
                "last_updated": datetime.now(tz=timezone.utc).isoformat(),
                "total_items": len(summaries),
                **(metadata or {}),
            },
        )
        self._storage.save(payload)
        return payload

