"""
Service orchestrating AI-powered summarisation.
"""

from __future__ import annotations

import json
from typing import Iterable, List, Optional, Sequence

from src.clients import GeminiClient
from src.config import settings
from src.domain import FeedArticle, SummaryRequest, SummaryResult
from src.utils import chunked, extract_json_block


class SummarizationService:
    """High-level service that turns feed articles into concise summaries."""

    def __init__(
        self,
        client: Optional[GeminiClient] = None,
        *,
        prompt_template: Optional[str] = None,
        batch_size: Optional[int] = None,
    ) -> None:
        cfg = settings.gemini
        self._client = client or GeminiClient(
            api_key=cfg.api_key,
            model=cfg.model,
            max_retries=cfg.max_retries,
        )
        self._prompt_template = prompt_template or settings.summary_prompt_template
        self._batch_size = batch_size or cfg.batch_size

    def summarize(self, articles: Sequence[FeedArticle]) -> List[SummaryResult]:
        requests = [article.to_summary_request() for article in articles]
        summaries: List[SummaryResult] = []
        for batch in chunked(requests, self._batch_size):
            prompt = self._build_prompt(batch)
            try:
                raw_response = self._client.generate(prompt)
                payload = extract_json_block(raw_response)
                summaries.extend(self._parse_summaries(payload))
            except json.JSONDecodeError:
                summaries.extend(self._fallback_summaries(batch))
            except Exception:
                summaries.extend(self._fallback_summaries(batch))
        return summaries

    def _build_prompt(self, requests: Sequence[SummaryRequest]) -> str:
        items_json = json.dumps(
            [req.to_prompt_dict() for req in requests],
            ensure_ascii=False,
        )
        return self._prompt_template.format(items_json=items_json)

    @staticmethod
    def _parse_summaries(payload: dict) -> Iterable[SummaryResult]:
        summaries = payload.get("summaries", [])
        return [SummaryResult.from_dict(item) for item in summaries]

    @staticmethod
    def _fallback_summaries(batch: Sequence[SummaryRequest]) -> Iterable[SummaryResult]:
        for item in batch:
            yield SummaryResult(
                title=item.title,
                url=item.url,
                bullets=["Summary not available due to parsing error"],
                why_it_matters="Content needs manual review",
                type="news",
            )

