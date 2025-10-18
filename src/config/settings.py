"""
Application configuration module.

Loads environment variables (via dotenv when available) and exposes a `Settings`
singleton with strongly-typed properties that are reused across services.
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path
from typing import Optional

try:
    from dotenv import load_dotenv  # type: ignore
except Exception:
    def load_dotenv(*args, **kwargs):
        """Fallback noop if python-dotenv is not installed."""
        return False

# Ensure .env values are available for local development.
load_dotenv()


@dataclass(frozen=True)
class GeminiConfig:
    api_key: str
    model: str
    max_retries: int
    batch_size: int


@dataclass(frozen=True)
class FeedConfig:
    url: str
    timeout: int
    user_agent: str


@dataclass(frozen=True)
class StorageConfig:
    output_path: Path


class Settings:
    """Centralised access to runtime configuration."""

    def __init__(
        self,
        *,
        gemini_api_key: Optional[str] = None,
        gemini_model: str = "gemini-2.5-flash",
        gemini_max_retries: int = 3,
        gemini_batch_size: int = 6,
        feed_url: Optional[str] = None,
        feed_timeout: int = 12,
        feed_user_agent: Optional[str] = None,
        output_path: Optional[Path] = None,
    ) -> None:
        root_dir = Path(__file__).resolve().parents[2]
        default_output = root_dir / "data" / "outputs" / "summaries.json"

        self._gemini = GeminiConfig(
            api_key=gemini_api_key or os.getenv("GEMINI_API_KEY", ""),
            model=os.getenv("GEMINI_MODEL", gemini_model),
            max_retries=int(os.getenv("GEMINI_MAX_RETRIES", gemini_max_retries)),
            batch_size=int(os.getenv("GEMINI_BATCH_SIZE", gemini_batch_size)),
        )

        self._feed = FeedConfig(
            url=os.getenv("TECHMEME_FEED_URL", feed_url or "https://www.techmeme.com/feed.xml"),
            timeout=int(os.getenv("TECHMEME_TIMEOUT", feed_timeout)),
            user_agent=os.getenv(
                "CRAWLER_UA",
                feed_user_agent
                or "Mozilla/5.0 (Windows NT 10.0; Win64; x64) TechHubBot/0.1 (+contact-you@example.com)",
            ),
        )

        self._storage = StorageConfig(
            output_path=Path(
                os.getenv("SUMMARY_OUTPUT_PATH", output_path or default_output)
            )
        )

        prompt_template = os.getenv("SUMMARY_PROMPT_TEMPLATE")
        self._prompt_template = prompt_template or _DEFAULT_PROMPT_TEMPLATE

    @property
    def gemini(self) -> GeminiConfig:
        if not self._gemini.api_key or self._gemini.api_key == "your_gemini_api_key_here":
            raise ValueError("GEMINI_API_KEY is required. Please set it in .env.")
        return self._gemini

    @property
    def feed(self) -> FeedConfig:
        return self._feed

    @property
    def storage(self) -> StorageConfig:
        return self._storage

    @property
    def summary_prompt_template(self) -> str:
        return self._prompt_template


_DEFAULT_PROMPT_TEMPLATE = """You are an expert tech news summarizer. Given a list of tech news items with title, url, and content text, generate a concise summary for each item.
REQUIREMENTS:
- Valid JSON output: {{"summaries":[{{...}}]}}
 - Each item: A few key bullets (<=80 characters for each bullet), add "why_it_matters".
- If troubleshooting: add "key_commands" & "caveats".
- Rephrase in an easy-to-understand way.
- You can use either English or Vietnamese.
- No fabrication. If information is missing, leave the array empty.
- Keep the "type" field as one of: news, howto, troubleshooting, announcement, video.

INPUT:
{items_json}

OUTPUT:
{{
  "summaries": [
    {{
      "title": "...",
      "url": "...",
      "bullets": ["...", "..."],
      "why_it_matters": "...",
      "type": "news|howto|troubleshooting|announcement|video",
      "key_commands": ["..."],
      "caveats": ["..."]
    }}
  ]
}}
"""


# Shared singleton instance
settings = Settings()

