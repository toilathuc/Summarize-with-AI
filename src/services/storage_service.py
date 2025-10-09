"""
Service responsible for persisting summaries to disk.
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Dict, Optional

from src.config import settings
from src.domain import SummaryPayload, SummaryResult


class StorageService:
    """Handles serialisation of summaries for the frontend."""

    def __init__(self, *, output_path: Optional[Path] = None) -> None:
        self._output_path = output_path or settings.storage.output_path

    @property
    def output_path(self) -> Path:
        return self._output_path

    def save(self, payload: SummaryPayload) -> Path:
        path = self._output_path
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("w", encoding="utf-8") as handle:
            json.dump(payload.to_dict(), handle, ensure_ascii=False, indent=2)
        return path

    def load_existing(self) -> SummaryPayload:
        path = self._output_path
        if not path.exists():
            return SummaryPayload.empty()
        with path.open("r", encoding="utf-8") as handle:
            data: Dict[str, Any] = json.load(handle)
        items = data.get("items", [])
        extra = {k: v for k, v in data.items() if k != "items"}
        summaries = [SummaryResult.from_dict(item) for item in items]
        return SummaryPayload(summaries=summaries, extra=extra)

