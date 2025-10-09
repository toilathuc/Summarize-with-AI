"""
Domain models describe the core data structures exchanged between services.
"""

from .models import FeedArticle, SummaryPayload, SummaryRequest, SummaryResult

__all__ = [
    "FeedArticle",
    "SummaryPayload",
    "SummaryRequest",
    "SummaryResult",
]
