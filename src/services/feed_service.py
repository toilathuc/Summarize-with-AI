"""
Service responsible for fetching domain articles from Techmeme.
"""

from __future__ import annotations

from typing import List, Optional

from src.domain import FeedArticle
from src.feeds.techmeme import TechmemeFeedClient


class FeedService:
    """Facade encapsulating Techmeme feed retrieval."""

    def __init__(self, client: Optional[TechmemeFeedClient] = None) -> None:
        self._client = client or TechmemeFeedClient()

    def fetch_latest(self, *, limit: Optional[int] = None) -> List[FeedArticle]:
        return self._client.fetch_articles(limit=limit)

