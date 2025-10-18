#!/usr/bin/env python3
"""Lightweight helper to simulate a refresh without calling external APIs."""

from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path

SUMMARIES_PATH = Path("summaries.json")


def quick_refresh() -> bool:
    """Touch the existing summaries file with a new timestamp for smoke tests."""
    if not SUMMARIES_PATH.exists():
        print("summaries.json not found - run update_news.py first.")
        return False

    try:
        with SUMMARIES_PATH.open("r", encoding="utf-8") as handle:
            payload = json.load(handle)
    except json.JSONDecodeError as exc:
        print(f"Could not parse summaries.json: {exc}")
        return False

    payload["last_updated"] = datetime.now(tz=timezone.utc).isoformat()
    items = payload.get("items", [])
    if isinstance(items, list) and items:
        title = items[0].get("title") or ""
        items[0]["title"] = f"[REFRESHED] {title}"

    with SUMMARIES_PATH.open("w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=2)

    print(f"Quick refresh completed at {payload['last_updated']}")
    return True


if __name__ == "__main__":
    raise SystemExit(0 if quick_refresh() else 1)
