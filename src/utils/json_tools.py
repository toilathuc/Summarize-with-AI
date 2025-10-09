"""
Helpers for dealing with model responses that wrap JSON inside code fences.
"""

from __future__ import annotations

import json
from typing import Any, Dict


def extract_json_block(response_text: str) -> Dict[str, Any]:
    """
    Extract JSON content from a raw model response.

    Gemini frequently wraps JSON inside markdown code fences. This helper
    normalises the payload and returns a parsed JSON dictionary.
    """

    text = response_text.strip()
    if text.startswith("```json"):
        text = text[len("```json") :].strip()
        if text.endswith("```"):
            text = text[: -len("```")].strip()
    elif text.startswith("```"):
        text = text[len("```") :].strip()
        if text.endswith("```"):
            text = text[: -len("```")].strip()
    return json.loads(text)

