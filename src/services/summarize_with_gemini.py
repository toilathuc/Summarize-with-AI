"""
Backward-compatible entry points for Gemini summarisation.

This module preserves the historical `summarize_items` function while delegating
the actual implementation to the new modular `SummarizationService`.
"""

from __future__ import annotations

from typing import Any, Iterable, List, Mapping, Optional, Sequence

from src.domain import FeedArticle, SummaryResult
from src.services.summarization_service import SummarizationService

_default_service = SummarizationService()


def summarize_items(items: Sequence[Mapping[str, Any]], batch_size: int | None = None) -> List[dict]:
    """
    Summarise a list of feed item dictionaries using Gemini.

    Parameters
    ----------
    items:
        Iterable of dictionaries containing the feed information (title, urls,
        content_text/summary_text) as produced by the feed client.
    batch_size:
        Optional override for the number of items processed per prompt.
    """

    articles = [_ensure_article(item) for item in items]
    service = _default_service if batch_size is None else SummarizationService(batch_size=batch_size)
    summaries = service.summarize(articles)
    return [summary.to_dict() for summary in summaries]


def summarize_articles(articles: Iterable[FeedArticle]) -> List[SummaryResult]:
    """Expose a typed helper for the new modular pipeline."""

    return _default_service.summarize(list(articles))


def summarize_content(content: str, *, title: str = "Ad-hoc summary", url: Optional[str] = None, max_length: Optional[int] = None) -> SummaryResult:
    """
    Convenience helper for quickly summarising a block of content.

    Returns the first summary result produced by the service. Primarily used by
    integration tests and diagnostics.
    """

    truncated = content[:max_length] if max_length else content
    article = FeedArticle(
        title=title,
        techmeme_url=url or "",
        original_url=url,
        summary_text=truncated,
        content_text=None,
        raw={"source": "adhoc"},
    )
    results = _default_service.summarize([article])
    return results[0] if results else SummaryResult(
        title=title,
        url=url,
        bullets=["Summary not available"],
        why_it_matters="Content needs manual review",
        type="news",
    )


def _ensure_article(raw: Mapping[str, Any]) -> FeedArticle:
    if isinstance(raw, FeedArticle):
        return raw
    return FeedArticle.from_dict(dict(raw))


__all__ = ["summarize_items", "summarize_articles", "summarize_content", "SummarizationService"]
