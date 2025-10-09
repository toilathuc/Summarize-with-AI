"""
Thin client around the Gemini Generative AI SDK.
"""

from __future__ import annotations

import time
from typing import Optional

import google.generativeai as genai


class GeminiClient:
    """Wrapper for executing prompts against a Gemini generative model."""

    def __init__(
        self,
        *,
        api_key: str,
        model: str,
        max_retries: int = 3,
        retry_backoff: float = 1.5,
    ) -> None:
        if not api_key or api_key == "your_gemini_api_key_here":
            raise ValueError("GEMINI_API_KEY is missing. Please configure the environment.")

        genai.configure(api_key=api_key)
        self._model_name = model
        self._model = genai.GenerativeModel(model)
        self._max_retries = max_retries
        self._retry_backoff = retry_backoff

    @property
    def model_name(self) -> str:
        return self._model_name

    def generate(self, prompt: str) -> str:
        """
        Execute a prompt against the Gemini model with retry logic.

        Returns the raw text response from the API.
        """
        for attempt in range(self._max_retries):
            try:
                response = self._model.generate_content(prompt)
                return response.text or ""
            except Exception:
                if attempt == self._max_retries - 1:
                    raise
                time.sleep(self._retry_backoff * (attempt + 1))
        return ""

