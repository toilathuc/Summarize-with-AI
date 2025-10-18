"""
Dataclasses representing the domain entities managed by the pipeline.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Dict, Iterable, List, Optional


@dataclass
class FeedArticle:
    """Normalized representation of a Techmeme entry."""

    title: str
    techmeme_url: str
    original_url: Optional[str] = None
    summary_text: Optional[str] = None
    content_text: Optional[str] = None
    hash: Optional[str] = None
    raw: Dict[str, Any] = field(default_factory=dict)

    @classmethod
    def from_dict(cls, payload: Dict[str, Any]) -> "FeedArticle":
        return cls(
            title=payload.get("title", ""),
            techmeme_url=payload.get("techmeme_url", ""),
            original_url=payload.get("original_url"),
            summary_text=payload.get("summary_text"),
            content_text=payload.get("content_text"),
            hash=payload.get("hash"),
            raw=payload,
        )

    def to_summary_request(self) -> "SummaryRequest":
        text = (self.content_text or self.summary_text or "")[:8000]
        return SummaryRequest(
            title=self.title,
            url=self.original_url or self.techmeme_url,
            text=text,
        )


@dataclass
class SummaryRequest:
    """Input payload for the summarisation service."""

    title: str
    url: Optional[str]
    text: str

    def to_prompt_dict(self) -> Dict[str, Any]:
        return {
            "title": self.title,
            "url": self.url,
            "text": self.text,
        }


@dataclass
class SummaryResult:
    """Structured summary returned by the summarisation service."""

    title: str
    url: Optional[str]
    bullets: List[str]
    why_it_matters: str
    type: str
    key_commands: List[str] = field(default_factory=list)
    caveats: List[str] = field(default_factory=list)

    @classmethod
    def from_dict(cls, payload: Dict[str, Any]) -> "SummaryResult":
        return cls(
            title=payload.get("title", ""),
            url=payload.get("url"),
            bullets=list(payload.get("bullets", [])),
            why_it_matters=payload.get("why_it_matters", ""),
            type=payload.get("type", "news"),
            key_commands=list(payload.get("key_commands", [])),
            caveats=list(payload.get("caveats", [])),
        )

    def to_dict(self) -> Dict[str, Any]:
        base = {
            "title": self.title,
            "url": self.url,
            "bullets": self.bullets,
            "why_it_matters": self.why_it_matters,
            "type": self.type,
        }
        if self.key_commands:
            base["key_commands"] = self.key_commands
        if self.caveats:
            base["caveats"] = self.caveats
        return base


@dataclass
class SummaryPayload:
    """Serializable payload persisted to disk for the frontend."""

    summaries: List[SummaryResult]
    extra: Dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> Dict[str, Any]:
        base = {"items": [summary.to_dict() for summary in self.summaries]}
        base.update(self.extra)
        return base

    @classmethod
    def empty(cls) -> "SummaryPayload":
        return cls(summaries=[])

    @classmethod
    def from_existing(cls, summaries: Iterable[Dict[str, Any]]) -> "SummaryPayload":
        return cls([SummaryResult.from_dict(entry) for entry in summaries])


