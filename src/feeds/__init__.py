"""
Feed interfaces exposed by the application.
"""

from .techmeme import TechmemeFeedClient
from .techmeme.client import (
    enrich_from_article,
    normalize_feed,
    as_plain_dict,
    extract_links,
    pick_links_from_entry_links,
    pick_links_from_html,
)

__all__ = [
    "TechmemeFeedClient",
    "enrich_from_article",
    "normalize_feed",
    "as_plain_dict",
    "extract_links",
    "pick_links_from_entry_links",
    "pick_links_from_html",
]
